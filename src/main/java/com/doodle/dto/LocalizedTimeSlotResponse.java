package com.doodle.dto;

import com.doodle.enums.SlotStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public record LocalizedTimeSlotResponse(
        @Schema(description = "Unique primary database auto-incrementing key token.", example = "1042")
        Long id,

        @Schema(description = "The unique email identifier string of the slot owner.", example = "alice.smith@example.com")
        String ownerId,

        @Schema(description = "The localized calendar start time string formatted to ISO-8601 specifications.", example = "2026-06-27T17:00:00")
        String startTime,

        @Schema(description = "The localized calendar end time string formatted to ISO-8601 specifications.", example = "2026-06-27T18:00:00")
        String endTime,

        @Schema(description = "The explicit IANA time zone identifier applied during calendar shifting calculations.", example = "Asia/Karachi")
        String timezoneId,

        @Schema(description = "The real-time availability status of the target time segment.")
        SlotStatus status
) {}