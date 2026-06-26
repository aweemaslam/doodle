package com.doodle.service.impl;

import com.doodle.model.TimeSlotEntity;
import com.doodle.model.UserEntity;
import com.doodle.repository.TimeSlotRepository;
import com.doodle.enums.SlotStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CalendarServiceImplTest {
    private static final Instant START = Instant.parse("2024-01-01T00:00:00Z");
    private static final Instant END = Instant.parse("2024-01-02T00:00:00Z");
    @Mock
    private TimeSlotRepository timeSlotRepository;

    @InjectMocks
    private CalendarServiceImpl calendarService;

    // ----------------------------
    // 1. GET AGGREGATED CALENDAR
    // ----------------------------
    @Test
    void shouldReturnAggregatedCalendar() {

        UserEntity user = new UserEntity();
        user.setEmail("user@example.com");

        TimeSlotEntity slot1 = new TimeSlotEntity();
        slot1.setId(1L);
        slot1.setOwner(user);
        slot1.setStartTime(Instant.parse("2024-01-01T10:00:00Z"));
        slot1.setEndTime(Instant.parse("2024-01-01T11:00:00Z"));
        slot1.setStatus(SlotStatus.FREE);

        TimeSlotEntity slot2 = new TimeSlotEntity();
        slot2.setId(2L);
        slot2.setOwner(user);
        slot2.setStartTime(Instant.parse("2024-01-01T14:00:00Z"));
        slot2.setEndTime(Instant.parse("2024-01-01T15:00:00Z"));
        slot2.setStatus(SlotStatus.RESERVED);

        when(timeSlotRepository.findByOwnerEmailAndStartTimeGreaterThanEqualAndEndTimeLessThanEqual(
                "user@example.com",
                Instant.parse("2024-01-01T00:00:00Z"),
                Instant.parse("2024-01-02T00:00:00Z")
        )).thenReturn(List.of(slot1, slot2));

        List<TimeSlotEntity> result = calendarService.getAggregatedCalendar(
                "user@example.com",
                Instant.parse("2024-01-01T00:00:00Z"),
                Instant.parse("2024-01-02T00:00:00Z"),
                "UTC"
        );

        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals(2L, result.get(1).getId());
    }

    // ----------------------------
    // 2. EMPTY CALENDAR
    // ----------------------------
    @Test
    void shouldReturnEmptyCalendarWhenNoSlots() {

        when(timeSlotRepository.findByOwnerEmailAndStartTimeGreaterThanEqualAndEndTimeLessThanEqual(
                anyString(), any(), any()
        )).thenReturn(List.of());

        List<TimeSlotEntity> result = calendarService.getAggregatedCalendar(
                "empty@example.com",
                Instant.now(),
                Instant.now().plusSeconds(86400),
                "UTC"
        );

        assertTrue(result.isEmpty());
    }

    // ----------------------------
    // 3. CALENDAR WITH DIFFERENT TIMEZONES
    // ----------------------------
    @Test
    void shouldHandleDifferentTimezones() {
        UserEntity user = new UserEntity();
        user.setEmail("user@example.com");

        TimeSlotEntity slot = new TimeSlotEntity();
        slot.setId(1L);
        slot.setOwner(user);
        slot.setStartTime(START);
        slot.setEndTime(END);
        slot.setStatus(SlotStatus.FREE);

        when(timeSlotRepository.findByOwnerEmailAndStartTimeGreaterThanEqualAndEndTimeLessThanEqual(
                eq("user@example.com"),
                any(Instant.class),
                any(Instant.class)
        )).thenReturn(List.of(slot));

        List<TimeSlotEntity> resultUTC = calendarService.getAggregatedCalendar(
                "user@example.com",
                START,
                END,
                "UTC"
        );

        List<TimeSlotEntity> resultNY = calendarService.getAggregatedCalendar(
                "user@example.com",
                START,
                END,
                "America/New_York"
        );

        assertEquals(1, resultUTC.size());
        assertEquals(1, resultNY.size());
    }

    // ----------------------------
    // 4. CALENDAR FILTERING BY PERIOD
    // ----------------------------
    @Test
    void shouldFilterSlotsByPeriod() {

        when(timeSlotRepository.findByOwnerEmailAndStartTimeGreaterThanEqualAndEndTimeLessThanEqual(
                "user@example.com", START, END
        )).thenReturn(List.of());

        List<TimeSlotEntity> result = calendarService.getAggregatedCalendar(
                "user@example.com",
                START,
                END,
                "UTC"
        );

        verify(timeSlotRepository, times(1))
                .findByOwnerEmailAndStartTimeGreaterThanEqualAndEndTimeLessThanEqual(
                        "user@example.com", START, END
                );

        assertTrue(result.isEmpty());
    }

    // ----------------------------
    // 5. SEPARATE FREE AND RESERVED SLOTS
    // ----------------------------
    @Test
    void shouldIncludeAllSlotStatusesInCalendar() {
        UserEntity user = new UserEntity();
        user.setEmail("user@example.com");

        TimeSlotEntity freeSlot = new TimeSlotEntity();
        freeSlot.setId(1L);
        freeSlot.setOwner(user);
        freeSlot.setStartTime(START);
        freeSlot.setEndTime(END);
        freeSlot.setStatus(SlotStatus.FREE);

        TimeSlotEntity reservedSlot = new TimeSlotEntity();
        reservedSlot.setId(2L);
        reservedSlot.setOwner(user);
        reservedSlot.setStartTime(START);
        reservedSlot.setEndTime(END);
        reservedSlot.setStatus(SlotStatus.RESERVED);

        TimeSlotEntity pendingSlot = new TimeSlotEntity();
        pendingSlot.setId(3L);
        pendingSlot.setOwner(user);
        pendingSlot.setStartTime(START);
        pendingSlot.setEndTime(END);
        pendingSlot.setStatus(SlotStatus.PENDING_RESERVATION);

        when(timeSlotRepository.findByOwnerEmailAndStartTimeGreaterThanEqualAndEndTimeLessThanEqual(
                anyString(), any(), any()
        )).thenReturn(List.of(freeSlot, reservedSlot, pendingSlot));

        List<TimeSlotEntity> result = calendarService.getAggregatedCalendar(
                "user@example.com",
                START,
                END,
                "Asia/Karachi"
        );

        assertEquals(3, result.size());
        assertEquals(SlotStatus.FREE, result.get(0).getStatus());
        assertEquals(SlotStatus.RESERVED, result.get(1).getStatus());
        assertEquals(SlotStatus.PENDING_RESERVATION, result.get(2).getStatus());
    }
}

