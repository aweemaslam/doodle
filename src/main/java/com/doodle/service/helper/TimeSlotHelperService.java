package com.doodle.service.helper;

import com.doodle.dto.BulkSlotRequest;
import com.doodle.dto.SlotRequest;
import com.doodle.dto.TimeSlotResponse;
import com.doodle.exception.InvalidMethodArgumentException;
import com.doodle.model.TimeSlotEntity;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;

/**
 * Common validation and transformation coordinator utility.
 * Eradicates cross-cutting parameter boilerplate and ensures data-type compliance.
 */
@Service
public class TimeSlotHelperService {

    private static final long NETWORK_LAG_MARGIN_SECONDS = 60;
    private static final String REDIS_KEY_PREFIX = "slot:state:";

    /**
     * Validates structural constraints for bulk slot generation requests.
     */
    public void validateBulkRequest(BulkSlotRequest request) {
        validateChronology(request.startTime(), request.endTime());
    }

    /**
     * Validates basic chronological boundaries for singular slot operations.
     */
    public void validateTimeBoundaries(SlotRequest request) {
        validateChronology(request.startTime(), request.endTime());
    }

    /**
     * Verifies if a given timezone string conforms to a valid IANA identifier.
     */
    public void validateTimezone(String timezoneId) {
        if (timezoneId == null || timezoneId.isBlank()) {
            throw new InvalidMethodArgumentException("Time zone identifier configuration cannot be blank or null.");
        }
        try {
            ZoneId.of(timezoneId);
        } catch (Exception e) {
            throw new InvalidMethodArgumentException("Invalid Time Zone ID: " + timezoneId);
        }
    }

    /**
     * Generates a standardized Redis state isolation key for a slot.
     */
    public String getRedisKey(Long id) {
        return REDIS_KEY_PREFIX + id;
    }

    /**
     * Transforms a managed Database Entity into a detached, timezone-aligned Data Transfer Object.
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

    /**
     * Shared private helper to eliminate duplicate date-time validations.
     */
    private void validateChronology(Instant startTime, Instant endTime) {
        if (startTime == null || endTime == null) {
            throw new InvalidMethodArgumentException("Start time and end time must be provided.");
        }

        // 1. Subtract the margin from the current time to create a "safe past boundary"
        Instant safePastBoundary = Instant.now().minusSeconds(NETWORK_LAG_MARGIN_SECONDS);

        // 2. Reject the request only if the timestamp is older than our safe boundary
        if (startTime.isBefore(safePastBoundary)) {
            throw new InvalidMethodArgumentException("Past timestamps are not allowed. Provide current or future times.");
        }

        if (!startTime.isBefore(endTime)) {
            throw new InvalidMethodArgumentException("Chronological Violation: Start time must occur prior to the designated end time.");
        }
    }
}