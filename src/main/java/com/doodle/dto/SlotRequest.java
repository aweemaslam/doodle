package com.doodle.dto;

import java.time.Instant;

public record SlotRequest(
        String ownerId,
        Instant startTime,
        Instant endTime,
        String timezoneId
) {}

