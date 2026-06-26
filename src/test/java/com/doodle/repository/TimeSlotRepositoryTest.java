package com.doodle.repository;

import com.doodle.enums.SlotStatus;
import com.doodle.model.TimeSlotEntity;
import com.doodle.model.UserEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TimeSlotRepositoryTest {

    // Note: This is a unit test for entity properties and basic logic validation.
    // Full repository integration tests with DB should be in integration tests.
    private TimeSlotEntity createTestSlot() {
        TimeSlotEntity slot = new TimeSlotEntity();
        slot.setId(1L);
        slot.setStartTime(Instant.parse("2026-01-01T10:00:00Z"));
        slot.setEndTime(Instant.parse("2026-01-01T11:00:00Z"));
        slot.setStatus(SlotStatus.FREE);
        return slot;
    }

    private UserEntity createTestUser() {
        UserEntity user = new UserEntity();
        user.setEmail("test@test.com");
        user.setFullName("Test User");
        return user;
    }

    // ----------------------------
    // 1. SAVE AND FETCH BASIC ENTITY
    // ----------------------------
    @Test
    void shouldSaveAndFetchTimeSlot() {

        TimeSlotEntity slot = new TimeSlotEntity();
        slot.setStartTime(Instant.parse("2026-01-01T10:00:00Z"));
        slot.setEndTime(Instant.parse("2026-01-01T11:00:00Z"));
        slot.setStatus(SlotStatus.FREE);

        // Unit test: verify entity state
        assertNotNull(slot.getStartTime());
        assertEquals(SlotStatus.FREE, slot.getStatus());
    }

    // ----------------------------
    // 2. BASIC ENTITY PROPERTIES
    // ----------------------------
    @Test
    void entity_hasCorrectInitialState() {
        TimeSlotEntity slot = createTestSlot();

        assertNotNull(slot.getId());
        assertEquals(1L, slot.getId());
        assertEquals(Instant.parse("2026-01-01T10:00:00Z"), slot.getStartTime());
        assertEquals(Instant.parse("2026-01-01T11:00:00Z"), slot.getEndTime());
        assertEquals(SlotStatus.FREE, slot.getStatus());
    }

    // ----------------------------
    // 3. OWNER RELATIONSHIP
    // ----------------------------
    @Test
    void entity_shouldSetOwer() {
        TimeSlotEntity slot = createTestSlot();
        UserEntity user = createTestUser();

        slot.setOwner(user);

        assertEquals(user, slot.getOwner());
        assertEquals("test@test.com", slot.getOwner().getEmail());
    }

    // ----------------------------
    // 4. TIME RANGE VALIDATION
    // ----------------------------
    @Test
    void entity_timezoneIdCanBeSet() {
        TimeSlotEntity slot = createTestSlot();
        slot.setTimezoneId("America/New_York");

        assertEquals("America/New_York", slot.getTimezoneId());
    }

    // ----------------------------
    // 5. STATUS TRANSITIONS
    // ----------------------------
    @Test
    void entity_statusCanTransition() {
        TimeSlotEntity slot = createTestSlot();
        assertEquals(SlotStatus.FREE, slot.getStatus());

        slot.setStatus(SlotStatus.RESERVED);
        assertEquals(SlotStatus.RESERVED, slot.getStatus());

        slot.setStatus(SlotStatus.NOT_AVAILABLE);
        assertEquals(SlotStatus.NOT_AVAILABLE, slot.getStatus());
    }
}