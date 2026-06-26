package com.doodle.dto;

import com.doodle.enums.SlotStatus;

import java.time.Instant;

public record TimeSlotResponse(
        Long id,
        String ownerId,
        Instant startTime,
        Instant endTime,
        String timezoneId,
        SlotStatus status
) {}