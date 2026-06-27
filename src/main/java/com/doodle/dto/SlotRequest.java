package com.doodle.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

/**
 * Clean data contract container capturing details needed to build or modify
 * an individual time segment allocation.
 */
@Schema(description = "Request payload capturing parameters needed to create or update an availability slot.")
public record SlotRequest(

        @NotBlank(message = "Owner email identifier cannot be null or blank.")
        @Email(message = "Owner ID must adhere to a valid email format pattern.")
        @Schema(description = "The unique email identifier of the slot creator.", requiredMode = Schema.RequiredMode.REQUIRED, example = "alice.smith@example.com")
        String ownerId,

        @NotNull(message = "Slot start timestamp is mandatory.")
        @Schema(description = "The absolute UTC start time marker.", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-06-27T14:00:00Z")
        Instant startTime,

        @NotNull(message = "Slot end timestamp is mandatory.")
        @Schema(description = "The absolute UTC end time marker.", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-06-27T15:00:00Z")
        Instant endTime,

        @NotBlank(message = "Target time zone configuration string cannot be blank.")
        @Schema(description = "The target IANA time zone identifier used for calendar translation.", requiredMode = Schema.RequiredMode.REQUIRED, example = "Asia/Karachi")
        String timezoneId
) {}
