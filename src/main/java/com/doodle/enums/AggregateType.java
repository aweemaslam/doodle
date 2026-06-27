package com.doodle.enums;

/**
 * Defines the domain boundary aggregates used to route and partition
 * transactional outbox event payloads across Kafka message streams.
 */
public enum AggregateType {
    /**
     * Time-slot management domain aggregate boundaries.
     */
    TIME_SLOT
}