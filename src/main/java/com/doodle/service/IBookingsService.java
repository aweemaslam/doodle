package com.doodle.service;

import com.doodle.dto.BookingRequest;
import com.doodle.dto.OutboxEntityPayload;

/**
 * Business orchestration contract interface managing high-concurrency
 * reservation handshakes and eventual database persistence mapping workflows.
 */
public interface IBookingsService {

    /**
     * Executes a fast-path atomic validation check and locks the target slot
     * status inside memory as PENDING_RESERVATION to protect against race conditions.
     *
     * @param slotId  The unique identification primary key of the targeted segment.
     * @param request The data payload wrapper containing title and participant criteria.
     */
    void markPendingReservationInRedis(Long slotId, BookingRequest request);

    /**
     * Intercepts asynchronous transactional messages from Kafka to finalize
     * the persistent relational database state and record confirmed meeting entities.
     *
     * @param payload The unpacked structural event payload containing booking details.
     */
    void performReservation(OutboxEntityPayload payload);
}
