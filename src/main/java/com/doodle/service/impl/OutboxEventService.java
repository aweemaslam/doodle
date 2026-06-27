package com.doodle.service.impl;


import com.doodle.dto.OutboxEntityPayload;
import com.doodle.enums.AggregateType;
import com.doodle.enums.OutboxEventType;
import com.doodle.model.OutboxEventEntity;
import com.doodle.repository.OutboxEventRepository;
import com.doodle.service.IOutboxEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transactional log interceptor service.
 * Appends outbox event entries within the same database transaction boundary as business operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxEventService implements IOutboxEventService {
    private final OutboxEventRepository outboxEventRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void saveOutbox(AggregateType aggregateType, String aggregateId, OutboxEventType eventType, OutboxEntityPayload payload) {
        log.debug("Recording transactional outbox entry. Aggregate ID: {}, Event Type: {}", aggregateId, eventType);

        OutboxEventEntity event = new OutboxEventEntity();
        event.setAggregateType(aggregateType);
        event.setAggregateId(aggregateId);
        event.setEventType(eventType);
        event.setPayload(payload);
        event.setProcessed(false);
        event.setRetryCount(0);
        event.setLastError(null);
        event.setActive(true);
        outboxEventRepository.save(event);
    }

}