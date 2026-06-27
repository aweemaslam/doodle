package com.doodle.kafka.model;

import com.doodle.dto.OutboxEntityPayload;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Immutable top-level structural envelope representing a unified Kafka cluster event message.
 * Emitted by the Outbox Pattern engine to communicate aggregate state changes across system boundaries.
 */
@Schema(description = "The absolute event record structure transmitted across Kafka messaging partitions.")
public record BookingEvent(

        @Schema(description = "The unique tracking business identity string of the source aggregate.", example = "1042")
        String aggregateId,

        @Schema(description = "The core domain classification name that generated the transaction.", example = "TIME_SLOT")
        String aggregateType,

        @Schema(description = "The explicit transactional type telling consumers what logic handler to execute.", example = "PENDING_RESERVATION_EVENT")
        String eventType,

        @Schema(description = "The detailed inner contextual business parameter data layout tree.")
        OutboxEntityPayload payload
) {
}