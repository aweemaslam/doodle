package com.doodle.service;

import com.doodle.dto.OutboxEntityPayload;
import com.doodle.enums.AggregateType;
import com.doodle.enums.OutboxEventType;

/**
 * Foundational execution contract for the Transactional Outbox Pattern subsystem.
 * Guarantees that critical state transition audit logs are saved reliably.
 */
public interface IOutboxEventService {

    /**
     * Records an uncompleted event entry atomically within the current transaction scope.
     * Deferring real broker delivery to an asynchronous relay worker.
     *
     * @param aggregateType The DDD aggregate root boundary identifier classification (e.g., TIME_SLOT).
     * @param aggregateId   The unique domain identity primary key string of the modified resource.
     * @param eventType      The functional classification type directing consumer logic.
     * @param payload       The structured data envelope mapping core parameters.
     */
    void saveOutbox(
            AggregateType aggregateType,
            String aggregateId,
            OutboxEventType eventType,
            OutboxEntityPayload payload
    );
}
