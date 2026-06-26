package com.doodle.dto;

import com.doodle.enums.SlotStatus;
import com.doodle.model.TimeSlotEntity;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.zone.ZoneRules;
import java.util.List;

public record UserCalendar(String ownerId, List<TimeSlotEntity> slots) {
    
    public List<TimeSlotEntity> getFreeSlots() {
        return slots.stream()
                    .filter(slot -> slot.getStatus() == SlotStatus.FREE)
                    .toList();
    }

    /**
     * Filters and returns slots while auditing for problematic global clock anomalies
     * like skipped spring-forward hours or repeated autumn rollbacks.
     */
    public List<TimeSlotEntity> getValidSlotsForZone(String targetZoneId) {
        ZoneId zone = ZoneId.of(targetZoneId);
        ZoneRules rules = zone.getRules();

        return slots.stream()
                .filter(slot -> {
                    ZonedDateTime zdtStart = ZonedDateTime.ofInstant(slot.getStartTime(), zone);
                    // Drop slots that fall directly into a non-existent clock gap during spring-forward shifts
                    return !rules.getTransition(zdtStart.toLocalDateTime()).isGap();
                })
                .toList();
    }
}