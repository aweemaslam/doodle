package com.doodle.service.impl;

import com.doodle.dto.BookingRequest;
import com.doodle.dto.OutboxEntityPayload;
import com.doodle.enums.SlotStatus;
import com.doodle.exception.SlotConflictException;
import com.doodle.exception.SlotNotFoundException;
import com.doodle.model.BookingEntity;
import com.doodle.model.TimeSlotEntity;
import com.doodle.repository.BookingRepository;
import com.doodle.repository.TimeSlotRepository;
import com.doodle.repository.UserRepository;
import com.doodle.service.IOutboxEventService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock
    private IOutboxEventService outboxEventService;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TimeSlotRepository timeSlotRepository;

    @InjectMocks
    private BookingServiceImpl bookingService;

    // ----------------------------
    // 1. SUCCESSFUL PENDING RESERVATION
    // ----------------------------
    @Test
    void shouldMarkPendingReservationSuccessfully() {
        BookingRequest request = mock(BookingRequest.class);
        when(userRepository.existsByEmailAndActiveTrue(any())).thenReturn(true);
        when(redisTemplate.execute(any(RedisScript.class), anyList()))
                .thenReturn(1L);

        // This call should run cleanly without exceptions
        assertDoesNotThrow(() -> bookingService.markPendingReservationInRedis(100L, request));

        verify(outboxEventService, times(1))
                .saveOutbox(any(), eq("100"), any(), any(OutboxEntityPayload.class));
    }

    // ----------------------------
    // 2. FAILED RESERVATION (slot not FREE)
    // ----------------------------
    @Test
    void shouldThrowSlotConflictExceptionWhenSlotNotFreeInRedis() {
        BookingRequest request = mock(BookingRequest.class);
        when(userRepository.existsByEmailAndActiveTrue(any())).thenReturn(true);
        when(redisTemplate.execute(any(RedisScript.class), anyList()))
                .thenReturn(0L); // Redis script indicates collision

        // Assert that the service correctly throws a SlotConflictException
        SlotConflictException exception = assertThrows(SlotConflictException.class, () ->
                bookingService.markPendingReservationInRedis(100L, request)
        );

        assertEquals("Booking conflict: Slot is already reserved or unavailable.", exception.getMessage());
        verifyNoInteractions(outboxEventService);
    }

    // ----------------------------
    // 3. IDENTITY GUARD (booking already exists)
    // ----------------------------
    @Test
    void shouldSkipIfBookingAlreadyExists() {
        OutboxEntityPayload payload = mock(OutboxEntityPayload.class);
        when(payload.slotId()).thenReturn(10L);

        when(bookingRepository.existsByTimeSlotIdAndActiveTrue(10L)).thenReturn(true);

        bookingService.performReservation(payload);

        verify(timeSlotRepository, never()).findByIdAndActiveTrue(any());
        verify(bookingRepository, never()).save(any());
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

        when(bookingRepository.existsByTimeSlotIdAndActiveTrue(10L)).thenReturn(false);
        when(timeSlotRepository.findByIdAndActiveTrue(10L)).thenReturn(Optional.of(slot));
        when(redisTemplate.execute(any(), anyList())).thenReturn(1L);

        bookingService.performReservation(payload);

        assertEquals(SlotStatus.RESERVED, slot.getStatus());

        verify(timeSlotRepository, times(1)).save(slot);
        verify(bookingRepository, times(1)).save(any(BookingEntity.class));
    }

    // ----------------------------
    // 5. SLOT NOT FOUND
    // ----------------------------
    @Test
    void shouldThrowWhenSlotNotFound() {
        OutboxEntityPayload payload = mock(OutboxEntityPayload.class);
        when(payload.slotId()).thenReturn(999L);

        when(bookingRepository.existsByTimeSlotIdAndActiveTrue(999L)).thenReturn(false);
        when(timeSlotRepository.findByIdAndActiveTrue(999L)).thenReturn(Optional.empty());

        assertThrows(SlotNotFoundException.class,
                () -> bookingService.performReservation(payload));
    }

    // ----------------------------
    // 6. VERIFY REDIS RESERVATION CALL
    // ----------------------------
    @Test
    void shouldCallRedisReservationScriptDuringFinalization() {
        // Arrange
        OutboxEntityPayload payload = mock(OutboxEntityPayload.class);
        BookingRequest request = mock(BookingRequest.class);

        when(payload.slotId()).thenReturn(1L);
        when(payload.request()).thenReturn(request);
        when(request.title()).thenReturn("Meeting");
        when(request.description()).thenReturn("Discussion");
        when(request.participants()).thenReturn(Set.of("alice@example.com"));

        TimeSlotEntity slot = new TimeSlotEntity();
        slot.setId(1L);
        slot.setStatus(SlotStatus.PENDING_RESERVATION);

        when(bookingRepository.existsByTimeSlotIdAndActiveTrue(1L)).thenReturn(false);
        when(timeSlotRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(slot));

        // Act
        assertDoesNotThrow(() -> bookingService.performReservation(payload));

        // Assert: Verify that the Redis script was executed with the expected key structure
        verify(redisTemplate, times(1)).execute(
                any(RedisScript.class),
                eq(List.of("slot:state:1"))
        );
    }
}