package com.doodle.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Structural event payload wrapper captured inside the transactional outbox table.
 * Serialized directly into PostgreSQL JSONB format and transmitted across Kafka broker clusters.
 */
@Schema(description = "Data payload wrapper for outbox storage and message distribution.")
public record OutboxEntityPayload(

        @NotNull(message = "Associated time slot ID cannot be null.")
        @Schema(description = "Primary tracking ID of the time slot being updated.", example = "1042")
        Long slotId,

        @Valid
        @NotNull(message = "Nested booking request details are mandatory.")
        @Schema(description = "The structural booking parameter layout containing title and participants details.")
        BookingRequest request
) {
}
