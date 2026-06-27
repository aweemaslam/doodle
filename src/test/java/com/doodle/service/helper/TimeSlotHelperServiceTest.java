package com.doodle.service.helper;

import com.doodle.dto.BulkSlotRequest;
import com.doodle.dto.SlotRequest;
import com.doodle.dto.TimeSlotResponse;
import com.doodle.exception.InvalidMethodArgumentException;
import com.doodle.model.TimeSlotEntity;
import com.doodle.model.UserEntity;
import com.doodle.enums.SlotStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;

class TimeSlotHelperServiceTest {

    private final TimeSlotHelperService helper = new TimeSlotHelperService();

    // ----------------------------
    // 1. TIMEZONE CONSTRAINTS
    // ----------------------------
    @Test
    void validateTimezone_rejectsNullOrBlank() {
        // null must be rejected with the specific blank/null text message
        InvalidMethodArgumentException nullEx = assertThrows(InvalidMethodArgumentException.class,
                () -> helper.validateTimezone(null));
        assertTrue(nullEx.getMessage().contains("cannot be blank or null"));

        // empty/blank strings must be rejected with the specific blank/null text message
        InvalidMethodArgumentException blankEx = assertThrows(InvalidMethodArgumentException.class,
                () -> helper.validateTimezone("   "));
        assertTrue(blankEx.getMessage().contains("cannot be blank or null"));
    }

    @Test
    void validateTimezone_acceptsValidIdentifiers() {
        // Valid structural IANA parameters should pass without error
        assertDoesNotThrow(() -> helper.validateTimezone("UTC"));
        assertDoesNotThrow(() -> helper.validateTimezone("Europe/London"));
        assertDoesNotThrow(() -> helper.validateTimezone("America/New_York"));
    }

    @Test
    void validateTimezone_rejectsInvalid() {
        // Unknown, corrupted, or non-IANA values should throw an error containing the specific mismatch string
        InvalidMethodArgumentException ex = assertThrows(InvalidMethodArgumentException.class,
                () -> helper.validateTimezone("Not/AZone"));
        assertTrue(ex.getMessage().contains("Invalid Time Zone ID: Not/AZone"));
    }

    // ----------------------------
    // 2. BOUNDARY & CHRONOLOGY VALIDATIONS
    // ----------------------------
    @Test
    void validateTimeBoundaries_andBulkRequest() {
        // Use active current markers to pass the Network Lag safety threshold successfully
        Instant startTimeNow = Instant.now().plusSeconds(10);
        Instant endTimeLater = startTimeNow.plusSeconds(1800); // 30-minute block duration

        // Valid single slot configuration execution check
        SlotRequest valid = new SlotRequest("owner@example.com", startTimeNow, endTimeLater, "UTC");
        assertDoesNotThrow(() -> helper.validateTimeBoundaries(valid));

        // Chronological violation validation check: End time matches or precedes the starting timestamp
        SlotRequest chronologicalViolation = new SlotRequest("owner@example.com", endTimeLater, startTimeNow, "UTC");
        InvalidMethodArgumentException chronoEx = assertThrows(InvalidMethodArgumentException.class,
                () -> helper.validateTimeBoundaries(chronologicalViolation));
        assertTrue(chronoEx.getMessage().contains("Chronological Violation"));

        // Network Lag history boundary check: Timestamp represents the deep past (beyond 60 seconds margin)
        Instant deepPastTime = Instant.now().minusSeconds(120);
        SlotRequest pastViolation = new SlotRequest("owner@example.com", deepPastTime, endTimeLater, "UTC");
        InvalidMethodArgumentException pastEx = assertThrows(InvalidMethodArgumentException.class,
                () -> helper.validateTimeBoundaries(pastViolation));
        assertTrue(pastEx.getMessage().contains("Past timestamps are not allowed"));

        // Valid structural check for multi-block batch generations
        BulkSlotRequest okBulk = new BulkSlotRequest("owner@example.com", startTimeNow, endTimeLater, 2, "UTC");
        assertDoesNotThrow(() -> helper.validateBulkRequest(okBulk));
    }

    // ----------------------------
    // 3. TRANSFORMATIONS & DATA RESOLUTIONS
    // ----------------------------
    @Test
    void getRedisKey_and_mapToResponse() {
        // Arrange business entities
        UserEntity user = new UserEntity();
        user.setEmail("u@example.com");
        user.setFullName("Test User");
        user.setDefaultTimezone(ZoneId.systemDefault().getId());

        TimeSlotEntity entity = new TimeSlotEntity();
        entity.setId(10L);
        entity.setOwner(user);
        entity.setStartTime(Instant.parse("2026-01-01T00:00:00Z"));
        entity.setEndTime(Instant.parse("2026-01-01T00:30:00Z"));
        entity.setTimezoneId("UTC");
        entity.setStatus(SlotStatus.FREE);

        // Evaluate Redis isolated storage naming generation match
        assertEquals("slot:state:10", helper.getRedisKey(10L));

        // Evaluate domain transformation mappings
        TimeSlotResponse response = helper.mapToResponse(entity);
        assertEquals(10L, response.id());
        assertEquals("u@example.com", response.ownerId());
        assertEquals(SlotStatus.FREE, response.status());
        assertEquals("UTC", response.timezoneId());
    }
}
