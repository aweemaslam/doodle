package com.doodle.service.helper;

import com.doodle.dto.BulkSlotRequest;
import com.doodle.dto.SlotRequest;
import com.doodle.dto.TimeSlotResponse;
import com.doodle.model.TimeSlotEntity;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.time.ZoneId;

@Service
public class TimeSlotHelperService {

    private static final String REDIS_KEY_PREFIX = "slot:state:";

    /**
     * Validates structural constraints for bulk slot generation requests.
     */
    public void validateBulkRequest(BulkSlotRequest request) {
        if (request.numberOfSlots() <= 0) {
            throw new IllegalArgumentException("The allocation requirement must demand at least 1 or more target slots.");
        }
        if (request.startTime() == null || request.endTime() == null) {
            throw new IllegalArgumentException("Absolute macro window tracking boundaries cannot be null.");
        }
        if (!request.startTime().isBefore(request.endTime())) {
            throw new IllegalArgumentException("Chronological Violation: Master start frame must occur prior to the end frame.");
        }
    }

    /**
     * Validates basic chronological boundaries for singular slot operations.
     */
    public void validateTimeBoundaries(SlotRequest request) {
        if (request.startTime() == null || request.endTime() == null) {
            throw new IllegalArgumentException("Start time and end time boundaries must be provided.");
        }
        if (!request.startTime().isBefore(request.endTime())) {
            throw new IllegalArgumentException("Chronological Violation: Start time must occur strictly before the designated end time.");
        }
    }

    /**
     * Verifies if a given timezone string conforms to a valid IANA identifier.
     */
    public void validateTimezone(String timezoneId) {
        if (timezoneId != null) {
            try {
                ZoneId.of(timezoneId);
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid IANA Time Zone ID identifier supplied: %s".formatted(timezoneId));
            }
        }
    }

    /**
     * Generates a standardized Redis state isolation key for a slot.
     */
    public String getRedisKey(Long id) {
        return REDIS_KEY_PREFIX + id;
    }

    /**
     * Transforms a managed Database Entity into a detached Data Transfer Object.
     */
    public TimeSlotResponse mapToResponse(TimeSlotEntity entity) {
        return new TimeSlotResponse(
                entity.getId(),
                entity.getOwner().getEmail(),
                entity.getStartTime(),
                entity.getEndTime(),
                entity.getTimezoneId(),
                entity.getStatus()
        );
    }
}