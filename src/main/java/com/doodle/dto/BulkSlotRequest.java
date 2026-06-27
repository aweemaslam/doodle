package com.doodle.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

/**
 * Clean data contract container capturing parameters needed to divide a continuous
 * time frame into equal-length bookable availability slots.
 */
public record BulkSlotRequest(

        @NotBlank(message = "Owner email identifier string cannot be null or blank.")
        @Email(message = "Owner identifier must adhere to a valid email formatting pattern.")
        String ownerId,

        @NotNull(message = "Timeline start boundary timestamp is mandatory.")
        Instant startTime,

        @NotNull(message = "Timeline end boundary timestamp is mandatory.")
        Instant endTime,

        @Min(value = 1, message = "Bulk generation requires at least 1 or more slots to be allocated.")
        int numberOfSlots,

        String timezoneId
) {}