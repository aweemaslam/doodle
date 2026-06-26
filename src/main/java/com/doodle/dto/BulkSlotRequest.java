package com.doodle.dto;

import java.time.Instant;

public record BulkSlotRequest(
        String ownerId,
        Instant startTime,
        Instant endTime,
        int numberOfSlots,
        String timezoneId
) {}