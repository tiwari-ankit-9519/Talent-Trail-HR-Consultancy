package com.abchr.platform.shared.outbox;

import com.abchr.platform.shared.config.AppProperties;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OutboxRelay {

    private static final Logger LOG = LogManager.getLogger(OutboxRelay.class);
    private static final int MAX_ATTEMPTS = 10;

    private final OutboxEventRepository repository;
    private final RabbitTemplate rabbitTemplate;
    private final AppProperties properties;

    public OutboxRelay(OutboxEventRepository repository, RabbitTemplate rabbitTemplate, AppProperties properties) {
        this.repository = repository;
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms}")
    @Transactional
    public void relayPending() {
        List<OutboxEvent> batch = repository.lockNextPending(properties.outbox().batchSize());
        for (OutboxEvent event : batch) {
            try {
                rabbitTemplate.send(properties.outbox().exchange(), event.getEventType(), toMessage(event));
                event.markPublished();
            } catch (RuntimeException ex) {
                event.markAttemptFailed(ex.getMessage(), MAX_ATTEMPTS);
                LOG.warn("Outbox event {} failed on attempt {}", event.getId(), event.getAttempts(), ex);
            }
        }
    }

    private Message toMessage(OutboxEvent event) {
        return MessageBuilder.withBody(event.getPayload().getBytes(StandardCharsets.UTF_8))
                .setContentType("application/json")
                .setContentEncoding(StandardCharsets.UTF_8.name())
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                .setMessageId(event.getId().toString())
                .setHeader("aggregateType", event.getAggregateType())
                .setHeader("aggregateId", event.getAggregateId())
                .build();
    }
}
