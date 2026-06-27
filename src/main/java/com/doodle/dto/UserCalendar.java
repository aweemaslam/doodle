package com.doodle.dto;

import com.doodle.model.TimeSlotEntity;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.zone.ZoneRules;
import java.util.List;

public record UserCalendar(String ownerId, List<TimeSlotEntity> slots) {

    /**
     * Filters out non-existent DST clock gap slots and converts the remaining
     * slots into localized response payloads for the target timezone.
     */
    public List<LocalizedTimeSlotResponse> getValidLocalizedSlots(String targetZoneId) {
        ZoneId zone = ZoneId.of(targetZoneId);
        ZoneRules rules = zone.getRules();

        return slots.stream()
                .filter(slot -> isValidLocalTime(slot, zone, rules))
                .map(slot -> mapToLocalizedResponse(slot, zone))
                .toList();
    }

    /**
     * Checks if a UTC slot falls into a non-existent local time gap (Spring Forward).
     */
    private boolean isValidLocalTime(TimeSlotEntity slot, ZoneId zone, ZoneRules rules) {
        // Fix: Convert to raw LocalDateTime first to capture original intended hours without internal JVM shifting
        LocalDateTime localDateTime = LocalDateTime.ofInstant(slot.getStartTime(), zone);
        var transition = rules.getTransition(localDateTime);
        return transition == null || !transition.isGap();
    }

    /**
     * Maps the database entity into a localized response payload.
     */
    private LocalizedTimeSlotResponse mapToLocalizedResponse(TimeSlotEntity entity, ZoneId zone) {
        // Dynamically compute the localized strings factoring in calendar rules
        String localStart = ZonedDateTime.ofInstant(entity.getStartTime(), zone).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String localEnd = ZonedDateTime.ofInstant(entity.getEndTime(), zone).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        return new LocalizedTimeSlotResponse(
                entity.getId(),
                entity.getOwner().getEmail(),
                localStart, // Pass localized string to frontend
                localEnd,   // Pass localized string to frontend
                zone.getId(),
                entity.getStatus()
        );
    }
}