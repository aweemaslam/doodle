package com.doodle.service;

import com.doodle.dto.OutboxEntityPayload;
import com.doodle.enums.AggregateType;
import com.doodle.enums.OutboxEventType;

public interface IOutboxEventService {
    void saveOutbox(AggregateType aggregateType, String aggregateId, OutboxEventType eventType, OutboxEntityPayload payload);
}
