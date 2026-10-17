package com.abchr.platform.shared.mail;

import com.abchr.platform.shared.security.Actor;
import com.abchr.platform.shared.web.RequestIdFilter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class EmailWorker {

    private static final Logger LOG = LogManager.getLogger(EmailWorker.class);

    private final Map<String, EmailHandler> handlers = new HashMap<>();
    private final ObjectMapper objectMapper;

    public EmailWorker(List<EmailHandler> registered, ObjectMapper objectMapper) {
        registered.forEach(handler -> handlers.put(handler.kind(), handler));
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = MailQueueConfig.QUEUE)
    public void onMessage(Message message) {
        EmailJob job;
        try {
            job = objectMapper.readValue(new String(message.getBody(), StandardCharsets.UTF_8), EmailJob.class);
        } catch (RuntimeException ex) {
            LOG.error("Dropping unreadable email job", ex);
            return;
        }
        EmailHandler handler = handlers.get(job.kind());
        if (handler == null) {
            LOG.error("No handler for email job kind {}", job.kind());
            return;
        }
        UUID id = UUID.fromString(job.id());
        List<String> args = job.args() == null ? List.of() : job.args();
        ThreadContext.put(RequestIdFilter.MDC_KEY, job.requestId() == null ? "job-" + id : job.requestId());
        ThreadContext.put(RequestIdFilter.MDC_USER_EMAIL_KEY, job.actor() == null ? "system" : job.actor());
        ThreadContext.put("httpMethod", "JOB");
        ThreadContext.put("httpPath", "email/" + job.kind());
        Actor.runAs(job.actor(), () -> {
            try {
                handler.handle(id, args);
            } catch (RuntimeException ex) {
                LOG.error("Email job {} {} failed", job.kind(), id, ex);
                try {
                    handler.fail(id, args, ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
                } catch (RuntimeException inner) {
                    LOG.error("Could not record failure of email job {} {}", job.kind(), id, inner);
                }
            } finally {
                ThreadContext.remove(RequestIdFilter.MDC_KEY);
                ThreadContext.remove(RequestIdFilter.MDC_USER_EMAIL_KEY);
                ThreadContext.remove("httpMethod");
                ThreadContext.remove("httpPath");
            }
        });
    }
}
