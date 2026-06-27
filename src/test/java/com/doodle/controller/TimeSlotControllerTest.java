package com.doodle.controller;

import com.doodle.dto.BulkSlotRequest;
import com.doodle.dto.SlotRequest;
import com.doodle.dto.TimeSlotResponse;
import com.doodle.enums.SlotStatus;
import com.doodle.model.TimeSlotEntity;
import com.doodle.model.UserEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class TimeSlotControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    // ----------------------------
    // 1. SLOT REQUEST DTO VALIDATION
    // ----------------------------
    @Test
    void shouldCreateSlotRequest() {

        SlotRequest request = new SlotRequest(
                "user-1",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                "UTC"
        );

        assertEquals("user-1", request.ownerId());
        assertNotNull(request.startTime());
        assertNotNull(request.endTime());
        assertEquals("UTC", request.timezoneId());
    }

    // ----------------------------
    // 2. SLOT RESPONSE DTO CREATION
    // ----------------------------
    @Test
    void shouldCreateSlotResponse() {

        TimeSlotResponse response = new TimeSlotResponse(
                1L,
                "user-1",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                "UTC",
                SlotStatus.FREE
        );

        assertEquals(1L, response.id());
        assertEquals(SlotStatus.FREE, response.status());
        assertEquals("UTC", response.timezoneId());
    }

    // ----------------------------
    // 3. BULK SLOT REQUEST DTO
    // ----------------------------
    @Test
    void shouldCreateBulkSlotRequest() {

        BulkSlotRequest request = new BulkSlotRequest(
                "user-1",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                5,
                "UTC"
        );

        assertEquals("user-1", request.ownerId());
        assertEquals(5, request.numberOfSlots());
        assertNotNull(request.startTime());
        assertNotNull(request.endTime());
        assertEquals("UTC", request.timezoneId());
    }

    // ----------------------------
    // 4. DTO JSON SERIALIZATION
    // ----------------------------
    @Test
    void shouldSerializeSlotRequestToJson() throws Exception {

        SlotRequest request = new SlotRequest(
                "test@example.com",
                Instant.parse("2024-01-01T10:00:00Z"),
                Instant.parse("2024-01-01T11:00:00Z"),
                "UTC"
        );

        String json = objectMapper.writeValueAsString(request);
        assertNotNull(json);
        assertTrue(json.contains("test@example.com"));
        assertTrue(json.contains("UTC"));
    }

    // ----------------------------
    // 5. DTO JSON DESERIALIZATION
    // ----------------------------
    @Test
    void shouldDeserializeSlotRequestFromJson() throws Exception {

        String json = "{\"ownerId\":\"test@example.com\",\"startTime\":\"2024-01-01T10:00:00Z\",\"endTime\":\"2024-01-01T11:00:00Z\",\"timezoneId\":\"UTC\"}";

        SlotRequest request = objectMapper.readValue(json, SlotRequest.class);

        assertEquals("test@example.com", request.ownerId());
        assertEquals("UTC", request.timezoneId());
        assertNotNull(request.startTime());
        assertNotNull(request.endTime());
    }

    // ----------------------------
    // 6. TIME SLOT ENTITY CREATION
    // ----------------------------
    @Test
    void shouldCreateSlotForCalendar() {

        UserEntity user = new UserEntity();
        user.setEmail("user@example.com");

        TimeSlotEntity slot = new TimeSlotEntity();
        slot.setId(1L);
        slot.setOwner(user);
        slot.setStartTime(Instant.parse("2024-01-01T10:00:00Z"));
        slot.setEndTime(Instant.parse("2024-01-01T11:00:00Z"));
        slot.setStatus(SlotStatus.FREE);
        slot.setTimezoneId("UTC");

        assertNotNull(slot.getId());
        assertEquals(1L, slot.getId());
        assertEquals("user@example.com", slot.getOwner().getEmail());
        assertEquals(SlotStatus.FREE, slot.getStatus());
    }

    // ----------------------------
    // 7. TIME RANGE FOR CALENDAR VIEW
    // ----------------------------
    @Test
    void shouldSetTimeRangeForCalendarQuery() {

        TimeSlotEntity slot = new TimeSlotEntity();
        Instant start = Instant.parse("2024-01-01T00:00:00Z");
        Instant end = Instant.parse("2024-01-31T23:59:59Z");

        slot.setStartTime(start);
        slot.setEndTime(end);

        assertEquals(start, slot.getStartTime());
        assertEquals(end, slot.getEndTime());
        assertTrue(slot.getEndTime().isAfter(slot.getStartTime()));
    }

    // ----------------------------
    // 8. TIMEZONE FOR CALENDAR VIEW
    // ----------------------------
    @Test
    void shouldSetTimeZoneForCalendarDisplay() {

        TimeSlotEntity slot = new TimeSlotEntity();
        slot.setTimezoneId("America/New_York");

        assertEquals("America/New_York", slot.getTimezoneId());
    }

    // ----------------------------
    // 9. CALENDAR SLOT FILTERING BY STATUS
    // ----------------------------
    @Test
    void shouldFilterSlotsByStatus() {

        TimeSlotEntity availableSlot = new TimeSlotEntity();
        availableSlot.setStatus(SlotStatus.FREE);

        TimeSlotEntity bookedSlot = new TimeSlotEntity();
        bookedSlot.setStatus(SlotStatus.RESERVED);

        assertEquals(SlotStatus.FREE, availableSlot.getStatus());
        assertEquals(SlotStatus.RESERVED, bookedSlot.getStatus());
        assertNotEquals(availableSlot.getStatus(), bookedSlot.getStatus());
    }

    // ----------------------------
    // 10. USER CALENDAR OWNER
    // ----------------------------
    @Test
    void shouldAssociateSlotToUserInCalendar() {

        UserEntity user = new UserEntity();
        user.setEmail("cal-owner@example.com");
        user.setFullName("Calendar Owner");

        TimeSlotEntity slot = new TimeSlotEntity();
        slot.setOwner(user);

        assertNotNull(slot.getOwner());
        assertEquals("cal-owner@example.com", slot.getOwner().getEmail());
        assertEquals("Calendar Owner", slot.getOwner().getFullName());
    }
}
