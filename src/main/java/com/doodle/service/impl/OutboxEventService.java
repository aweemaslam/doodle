package com.doodle.service.impl;


import com.doodle.dto.OutboxEntityPayload;
import com.doodle.enums.AggregateType;
import com.doodle.enums.OutboxEventType;
import com.doodle.model.OutboxEventEntity;
import com.doodle.repository.OutboxEventRepository;
import com.doodle.service.IOutboxEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OutboxEventService implements IOutboxEventService {
    private final OutboxEventRepository outboxEventRepository;

    @Override
    public void saveOutbox(AggregateType aggregateType, String aggregateId, OutboxEventType eventType, OutboxEntityPayload payload) {
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