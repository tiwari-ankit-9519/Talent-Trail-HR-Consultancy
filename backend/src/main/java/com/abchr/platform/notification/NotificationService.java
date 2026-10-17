package com.abchr.platform.notification;

import com.abchr.platform.shared.security.Actor;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.ObjectMapper;

@Service
public class NotificationService {

    private static final Logger LOG = LogManager.getLogger(NotificationService.class);

    public record Item(UUID id, String topic, String title, String body, String link, boolean read, Instant createdAt) {
    }

    public record Feed(long unread, List<Item> items) {
    }

    private final NotificationRepository repository;
    private final JdbcTemplate jdbc;
    private final RabbitTemplate rabbit;
    private final ObjectMapper objectMapper;

    public NotificationService(
            NotificationRepository repository, JdbcTemplate jdbc, RabbitTemplate rabbit, ObjectMapper objectMapper) {
        this.repository = repository;
        this.jdbc = jdbc;
        this.rabbit = rabbit;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void notifyUsers(Collection<UUID> recipients, String topic, String title, String body, String link) {
        Set<UUID> targets = clean(recipients);
        if (targets.isEmpty()) {
            return;
        }
        LivePayload last = null;
        for (UUID userId : targets) {
            Notification notification = repository.save(Notification.create(userId, topic, title, body, link));
            last = new LivePayload(topic, notification.id, title, body, link, notification.createdAt);
            publishAfterCommit(new LiveEvent(List.of(userId), last));
        }
    }

    public void refresh(Collection<UUID> recipients, String topic) {
        Set<UUID> targets = clean(recipients);
        if (targets.isEmpty()) {
            return;
        }
        publishAfterCommit(new LiveEvent(List.copyOf(targets), new LivePayload(topic, null, null, null, null, Instant.now())));
    }

    @Transactional(readOnly = true)
    public List<UUID> usersWithAnyPermission(String... codes) {
        String marks = String.join(",", java.util.Collections.nCopies(codes.length, "?"));
        return jdbc.query(
                "select distinct u.id from app_user u join user_role ur on ur.user_id = u.id "
                        + "join role_permission rp on rp.role_id = ur.role_id "
                        + "where u.status = 'ACTIVE' and rp.permission_code in (" + marks + ")",
                (rs, row) -> (UUID) rs.getObject(1),
                (Object[]) codes);
    }

    @Transactional(readOnly = true)
    public Feed feed(int limit) {
        UUID userId = currentUser();
        List<Item> items = repository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, Math.min(limit, 100)))
                .stream()
                .map(n -> new Item(n.id, n.topic, n.title, n.body, n.link, n.readAt != null, n.createdAt))
                .toList();
        return new Feed(repository.countByUserIdAndReadAtIsNull(userId), items);
    }

    @Transactional
    public void markRead(UUID id) {
        repository.markRead(id, currentUser(), Instant.now());
    }

    @Transactional
    public void markAllRead() {
        repository.markAllRead(currentUser(), Instant.now());
    }

    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void purge() {
        int removed = repository.deleteOlderThan(Instant.now().minus(60, ChronoUnit.DAYS));
        if (removed > 0) {
            LOG.info("Purged {} old notifications", removed);
        }
    }

    private Set<UUID> clean(Collection<UUID> recipients) {
        Set<UUID> targets = new LinkedHashSet<>(recipients);
        targets.remove(Actor.current().id());
        targets.remove(null);
        return targets;
    }

    private static UUID currentUser() {
        return Actor.current().id();
    }

    private void publishAfterCommit(LiveEvent event) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publish(event);
                }
            });
        } else {
            publish(event);
        }
    }

    private void publish(LiveEvent event) {
        try {
            byte[] body = objectMapper.writeValueAsBytes(event);
            MessageProperties props = new MessageProperties();
            props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
            rabbit.send(LiveConfig.EXCHANGE, "", MessageBuilder.withBody(body).andProperties(props).build());
        } catch (RuntimeException ex) {
            LOG.warn("Could not publish live event: {}", ex.toString());
        }
    }
}
