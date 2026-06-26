package com.doodle.repository;

import com.doodle.model.MeetingEntity;
import com.doodle.model.TimeSlotEntity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MeetingRepositoryTest {

    // ----------------------------
    // 1. CREATE MEETING WITH SLOT
    // ----------------------------
    @Test
    void shouldCreateMeetingWithSlot() {

        MeetingEntity meeting = new MeetingEntity();
        TimeSlotEntity slot = new TimeSlotEntity();
        slot.setId(1L);

        meeting.setTimeSlot(slot);

        // Verify entity state
        assertNotNull(meeting.getTimeSlot());
        assertEquals(1L, meeting.getTimeSlot().getId());
    }

    // ----------------------------
    // 2. MEETING STATE PROPERTIES
    // ----------------------------
    @Test
    void shouldSetMeetingProperties() {

        MeetingEntity meeting = new MeetingEntity();
        TimeSlotEntity slot = new TimeSlotEntity();
        slot.setId(5L);

        meeting.setTimeSlot(slot);
        meeting.setTitle("Team Sync");
        meeting.setDescription("Weekly sync");

        assertEquals("Team Sync", meeting.getTitle());
        assertEquals("Weekly sync", meeting.getDescription());
        assertEquals(5L, meeting.getTimeSlot().getId());
    }

    // ----------------------------
    // 3. MEETING CAN BE ASSOCIATED TO DIFFERENT SLOTS
    // ----------------------------
    @Test
    void shouldAllowChangingTimeSlot() {

        MeetingEntity meeting = new MeetingEntity();

        TimeSlotEntity slot1 = new TimeSlotEntity();
        slot1.setId(1L);
        meeting.setTimeSlot(slot1);
        assertEquals(1L, meeting.getTimeSlot().getId());

        TimeSlotEntity slot2 = new TimeSlotEntity();
        slot2.setId(2L);
        meeting.setTimeSlot(slot2);
        assertEquals(2L, meeting.getTimeSlot().getId());
    }
}
