package com.doodle.service.impl;

import com.doodle.dto.BookingRequest;
import com.doodle.dto.OutboxEntityPayload;
import com.doodle.enums.SlotStatus;
import com.doodle.exception.InvalidStatusTransitionException;
import com.doodle.exception.SlotNotFoundException;
import com.doodle.model.MeetingEntity;
import com.doodle.model.TimeSlotEntity;
import com.doodle.repository.MeetingRepository;
import com.doodle.repository.TimeSlotRepository;
import com.doodle.service.IOutboxEventService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MeetingServiceImplTest {

    @Mock
    private IOutboxEventService outboxEventService;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private MeetingRepository meetingRepository;

    @Mock
    private TimeSlotRepository timeSlotRepository;

    @InjectMocks
    private MeetingServiceImpl meetingService;

    // ----------------------------
    // 1. SUCCESSFUL PENDING RESERVATION
    // ----------------------------
    @Test
    void shouldMarkPendingReservationSuccessfully() {
        BookingRequest request = mock(BookingRequest.class);

        when(redisTemplate.execute(any(RedisScript.class), anyList()))
                .thenReturn(1L);

        boolean result = meetingService.markPendingReservationInRedis(100L, request);

        assertTrue(result);

        verify(outboxEventService, times(1))
                .saveOutbox(any(), eq("100"), any(), any(OutboxEntityPayload.class));
    }

    // ----------------------------
    // 2. FAILED RESERVATION (slot not FREE)
    // ----------------------------
    @Test
    void shouldReturnFalseWhenSlotNotFreeInRedis() {
        BookingRequest request = mock(BookingRequest.class);

        when(redisTemplate.execute(any(RedisScript.class), anyList()))
                .thenReturn(0L);

        boolean result = meetingService.markPendingReservationInRedis(100L, request);

        assertFalse(result);

        verifyNoInteractions(outboxEventService);
    }

    // ----------------------------
    // 3. IDENTITY GUARD (meeting already exists)
    // ----------------------------
    @Test
    void shouldSkipIfMeetingAlreadyExists() {
        OutboxEntityPayload payload = mock(OutboxEntityPayload.class);
        when(payload.slotId()).thenReturn(10L);

        when(meetingRepository.existsByTimeSlotId(10L)).thenReturn(true);

        meetingService.performReservation(payload);

        verify(timeSlotRepository, never()).findById(any());
        verify(meetingRepository, never()).save(any());
    }

    // ----------------------------
    // 4. SUCCESSFUL RESERVATION FLOW
    // ----------------------------
    @Test
    void shouldPerformReservationSuccessfully() {
        OutboxEntityPayload payload = mock(OutboxEntityPayload.class);
        BookingRequest request = mock(BookingRequest.class);

        when(payload.slotId()).thenReturn(10L);
        when(payload.request()).thenReturn(request);

        TimeSlotEntity slot = new TimeSlotEntity();
        slot.setStatus(SlotStatus.PENDING_RESERVATION);

        when(meetingRepository.existsByTimeSlotId(10L)).thenReturn(false);
        when(timeSlotRepository.findById(10L)).thenReturn(Optional.of(slot));
        when(redisTemplate.execute(
                any(), anyList()
        )).thenReturn(1L);
        meetingService.performReservation(payload);

        assertEquals(SlotStatus.RESERVED, slot.getStatus());

        verify(timeSlotRepository, times(1)).save(slot);
        verify(meetingRepository, times(1)).save(any(MeetingEntity.class));
    }

    // ----------------------------
    // 5. SLOT NOT FOUND
    // ----------------------------
    @Test
    void shouldThrowWhenSlotNotFound() {
        OutboxEntityPayload payload = mock(OutboxEntityPayload.class);
        when(payload.slotId()).thenReturn(999L);

        when(meetingRepository.existsByTimeSlotId(999L)).thenReturn(false);
        when(timeSlotRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(SlotNotFoundException.class,
                () -> meetingService.performReservation(payload));
    }

    // ----------------------------
    // 6. VERIFY REDIS RESERVATION CALL
    // ----------------------------
    @Test
    void shouldCallRedisReservationScriptDuringFinalization() {
        OutboxEntityPayload payload = mock(OutboxEntityPayload.class);
        BookingRequest request = mock(BookingRequest.class);

        when(payload.slotId()).thenReturn(1L);
        when(payload.request()).thenReturn(request);

        TimeSlotEntity slot = new TimeSlotEntity();

        when(meetingRepository.existsByTimeSlotId(1L)).thenReturn(false);
        when(timeSlotRepository.findById(1L)).thenReturn(Optional.of(slot));

        assertThrows(InvalidStatusTransitionException.class, () -> meetingService.performReservation(payload));
    }
}