package com.doodle.dto;

import com.doodle.enums.SlotStatus;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Clean data contract container capturing the result of a fast-path reservation booking action.
 */
@Schema(description = "Response payload returning the instant orchestration state of a slot booking action.")
public record BookingResponse(

        @Schema(description = "The transient operational availability state of the target slot.", requiredMode = Schema.RequiredMode.REQUIRED, example = "PENDING_RESERVATION")
        SlotStatus status,

        @Schema(description = "Descriptive confirmation details explaining processing tracking information.", requiredMode = Schema.RequiredMode.REQUIRED, example = "Slot reservation initiated successfully.")
        String message
) {}
