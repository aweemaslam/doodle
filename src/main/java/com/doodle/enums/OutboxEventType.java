package com.doodle.enums;

/**
 * Defines the specific classification of transactional outbox events.
 * Instructs downstream Kafka consumers which state transition logic to invoke.
 */
public enum OutboxEventType {
    /**
     * Fired when a user initiates a fast-path reservation in Redis.
     * Triggers asynchronous downstream database mapping routines to isolate the slot.
     */
    PENDING_RESERVATION_EVENT,

    /**
     * Fired when database persistence completes successfully and the meeting is finalized.
     * Triggers external secondary reactions like sending calendar invites or email notifications.
     */
    RESERVED_EVENT
}
