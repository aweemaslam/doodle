package com.doodle.kafka.model;


import com.doodle.dto.OutboxEntityPayload;

/**
 * Kafka payload for time-slot-related events. Converted to a Java record for
 * immutability and simple JSON (de)serialization.
 */
public record BookingEvent(
        String aggregateId,
        String aggregateType,
        String eventType,
        OutboxEntityPayload payload
) {
}