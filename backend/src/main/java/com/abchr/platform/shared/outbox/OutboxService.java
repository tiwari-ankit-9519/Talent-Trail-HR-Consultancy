package com.abchr.platform.shared.outbox;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboxService {

    private final OutboxEventRepository repository;

    public OutboxService(OutboxEventRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public OutboxEvent record(String aggregateType, String aggregateId, String eventType, String payloadJson) {
        return repository.save(new OutboxEvent(aggregateType, aggregateId, eventType, payloadJson));
    }
}
