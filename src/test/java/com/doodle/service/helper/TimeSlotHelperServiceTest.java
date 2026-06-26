package com.doodle.service.helper;

import com.doodle.dto.BulkSlotRequest;
import com.doodle.dto.SlotRequest;
import com.doodle.dto.TimeSlotResponse;
import com.doodle.model.TimeSlotEntity;
import com.doodle.model.UserEntity;
import com.doodle.enums.SlotStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;

class TimeSlotHelperServiceTest {

    private final TimeSlotHelperService helper = new TimeSlotHelperService();

    @Test
    void validateTimezone_acceptsNullAndValid() {
        // null should be allowed
        assertDoesNotThrow(() -> helper.validateTimezone(null));

        // valid timezone
        assertDoesNotThrow(() -> helper.validateTimezone("UTC"));
        assertDoesNotThrow(() -> helper.validateTimezone("Europe/London"));
    }

    @Test
    void validateTimezone_rejectsInvalid() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> helper.validateTimezone("Not/AZone"));
        assertTrue(ex.getMessage().contains("Invalid IANA Time Zone ID"));
    }

    @Test
    void validateTimeBoundaries_andBulkRequest() {
        Instant a = Instant.parse("2023-01-01T00:00:00Z");
        Instant b = Instant.parse("2023-01-01T01:00:00Z");

        // single slot valid
        SlotRequest valid = new SlotRequest("owner", a, b, "UTC");
        assertDoesNotThrow(() -> helper.validateTimeBoundaries(valid));

        // invalid where start >= end
        SlotRequest invalid = new SlotRequest("owner", b, a, "UTC");
        assertThrows(IllegalArgumentException.class, () -> helper.validateTimeBoundaries(invalid));

        // bulk request validations
        BulkSlotRequest okBulk = new BulkSlotRequest("owner", a, b, 2, "UTC");
        assertDoesNotThrow(() -> helper.validateBulkRequest(okBulk));

        BulkSlotRequest badNumber = new BulkSlotRequest("owner", a, b, 0, "UTC");
        assertThrows(IllegalArgumentException.class, () -> helper.validateBulkRequest(badNumber));
    }

    @Test
    void getRedisKey_and_mapToResponse() {
        UserEntity user = new UserEntity();
        user.setEmail("u@example.com");
        user.setFullName("Test User");
        user.setDefaultTimezone(ZoneId.systemDefault().getId());

        TimeSlotEntity e = new TimeSlotEntity();
        e.setId(10L);
        e.setOwner(user);
        e.setStartTime(Instant.parse("2023-01-01T00:00:00Z"));
        e.setEndTime(Instant.parse("2023-01-01T00:30:00Z"));
        e.setTimezoneId("UTC");
        e.setStatus(SlotStatus.FREE);

        assertEquals("slot:state:10", helper.getRedisKey(10L));

        TimeSlotResponse resp = helper.mapToResponse(e);
        assertEquals(10L, resp.id());
        assertEquals("u@example.com", resp.ownerId());
        assertEquals(SlotStatus.FREE, resp.status());
    }
}

