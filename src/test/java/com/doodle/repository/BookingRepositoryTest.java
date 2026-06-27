package com.doodle.repository;

import com.doodle.model.BookingEntity;
import com.doodle.model.TimeSlotEntity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BookingRepositoryTest {

    // ----------------------------
    // 1. CREATE BOOKING WITH SLOT
    // ----------------------------
    @Test
    void shouldCreateBookingWithSlot() {

        BookingEntity booking = new BookingEntity();
        TimeSlotEntity slot = new TimeSlotEntity();
        slot.setId(1L);

        booking.setTimeSlot(slot);

        // Verify entity state
        assertNotNull(booking.getTimeSlot());
        assertEquals(1L, booking.getTimeSlot().getId());
    }

    // ----------------------------
    // 2. BOOKING STATE PROPERTIES
    // ----------------------------
    @Test
    void shouldSetBookingProperties() {

        BookingEntity booking = new BookingEntity();
        TimeSlotEntity slot = new TimeSlotEntity();
        slot.setId(5L);

        booking.setTimeSlot(slot);
        booking.setTitle("Team Sync");
        booking.setDescription("Weekly sync");

        assertEquals("Team Sync", booking.getTitle());
        assertEquals("Weekly sync", booking.getDescription());
        assertEquals(5L, booking.getTimeSlot().getId());
    }

    // ----------------------------
    // 3. BOOKING CAN BE ASSOCIATED TO DIFFERENT SLOTS
    // ----------------------------
    @Test
    void shouldAllowChangingTimeSlot() {

        BookingEntity booking = new BookingEntity();

        TimeSlotEntity slot1 = new TimeSlotEntity();
        slot1.setId(1L);
        booking.setTimeSlot(slot1);
        assertEquals(1L, booking.getTimeSlot().getId());

        TimeSlotEntity slot2 = new TimeSlotEntity();
        slot2.setId(2L);
        booking.setTimeSlot(slot2);
        assertEquals(2L, booking.getTimeSlot().getId());
    }
}
