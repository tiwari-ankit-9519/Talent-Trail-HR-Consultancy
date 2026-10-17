package com.abchr.platform.shared.mail;

import com.abchr.platform.shared.outbox.OutboxService;
import com.abchr.platform.shared.web.RequestIdFilter;
import java.util.List;
import java.util.UUID;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
public class EmailQueue {

    public static final String ROUTING_PREFIX = "email.";

    private final OutboxService outboxService;
    private final ObjectMapper objectMapper;

    public EmailQueue(OutboxService outboxService, ObjectMapper objectMapper) {
        this.outboxService = outboxService;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueue(String kind, UUID id, String actorEmail, List<String> args) {
        EmailJob job = new EmailJob(kind, id.toString(), actorEmail, args == null ? List.of() : args,
                ThreadContext.get(RequestIdFilter.MDC_KEY));
        outboxService.record("email", id.toString(), ROUTING_PREFIX + kind.toLowerCase(),
                objectMapper.writeValueAsString(job));
    }
}
