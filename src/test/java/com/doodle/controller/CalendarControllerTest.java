package com.doodle.controller;

import com.doodle.enums.SlotStatus;
import com.doodle.model.TimeSlotEntity;
import com.doodle.model.UserEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class CalendarControllerTest {

    // ----------------------------
    // 1. TIME SLOT ENTITY CREATION
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
    // 2. TIME RANGE FOR CALENDAR VIEW
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
    // 3. TIMEZONE FOR CALENDAR VIEW
    // ----------------------------
    @Test
    void shouldSetTimeZoneForCalendarDisplay() {

        TimeSlotEntity slot = new TimeSlotEntity();
        slot.setTimezoneId("America/New_York");

        assertEquals("America/New_York", slot.getTimezoneId());
    }

    // ----------------------------
    // 4. CALENDAR SLOT FILTERING BY STATUS
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
    // 5. USER CALENDAR OWNER
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
