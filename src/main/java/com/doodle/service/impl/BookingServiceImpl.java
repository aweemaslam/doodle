package com.doodle.service.impl;

import com.doodle.dto.BookingRequest;
import com.doodle.dto.OutboxEntityPayload;
import com.doodle.enums.AggregateType;
import com.doodle.enums.OutboxEventType;
import com.doodle.enums.SlotStatus;
import com.doodle.exception.ResourceNotFoundException;
import com.doodle.exception.SlotNotFoundException;
import com.doodle.exception.SlotConflictException;
import com.doodle.model.BookingEntity;
import com.doodle.model.TimeSlotEntity;
import com.doodle.repository.BookingRepository;
import com.doodle.repository.TimeSlotRepository;
import com.doodle.repository.UserRepository;
import com.doodle.service.IBookingsService;
import com.doodle.service.IOutboxEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements IBookingsService {

    private final IOutboxEventService outboxEventService;
    private final StringRedisTemplate redisTemplate;
    private final BookingRepository bookingRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final UserRepository userRepository;

    private static final String REDIS_KEY_PREFIX = "slot:state:";

    private static final RedisScript<Long> PENDING_RESERVATION_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('GET', KEYS[1]) == 'FREE' then " +
                    "    redis.call('SET', KEYS[1], 'PENDING_RESERVATION') " +
                    "    return 1 " +
                    "else " +
                    "    return 0 " +
                    "end",
            Long.class
    );

    private static final RedisScript<Long> REVERT_PENDING_RESERVATION_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('GET', KEYS[1]) == 'PENDING_RESERVATION' then " +
                    "    redis.call('SET', KEYS[1], 'FREE') " +
                    "    return 1 " +
                    "else " +
                    "    return 0 " +
                    "end",
            Long.class
    );

    private static final RedisScript<Long> RESERVATION_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('GET', KEYS[1]) == 'PENDING_RESERVATION' then " +
                    "    redis.call('SET', KEYS[1], 'RESERVED') " +
                    "    return 1 " +
                    "else " +
                    "    return 0 " +
                    "end",
            Long.class
    );

    @Override
    @Transactional
    public void markPendingReservationInRedis(Long slotId, BookingRequest request) {
        if (!userRepository.existsByEmailAndActiveTrue(request.ownerId())) {
            throw new ResourceNotFoundException("Owner missing or deactivated: %s".formatted(request.ownerId()));
        }

        final String slotKey = REDIS_KEY_PREFIX + slotId;

        // Execute Atomic state lock inside Redis instantly
        Long redisResult = redisTemplate.execute(PENDING_RESERVATION_SCRIPT, List.of(slotKey));

        if (!Objects.equals(redisResult, 1L)) {
            throw new SlotConflictException("Booking conflict: Slot is already reserved or unavailable.");
        }

        try {
            // Persist to outbox atomically within database boundaries
            outboxEventService.saveOutbox(
                    AggregateType.TIME_SLOT,
                    String.valueOf(slotId),
                    OutboxEventType.PENDING_RESERVATION_EVENT,
                    new OutboxEntityPayload(slotId, request)
            );
        } catch (Exception e) {
            log.error("Database outbox write failed. Initiating automatic Redis rollback compensation for key: {}", slotKey, e);
            // Compensating transaction: Revert Redis state if DB fails to protect cache consistency
            redisTemplate.execute(REVERT_PENDING_RESERVATION_SCRIPT, List.of(slotKey));
            throw e;
        }
    }

    @Override
    @Transactional
    public void performReservation(OutboxEntityPayload payload) {
        // Idempotency check: Protect against duplicate Kafka message deliveries
        if (bookingRepository.existsByTimeSlotIdAndActiveTrue(payload.slotId())) {
            log.warn("Idempotency match found. Skipping duplicate reservation mapping processing for slot: {}", payload.slotId());
            return;
        }

        TimeSlotEntity slot = timeSlotRepository.findByIdAndActiveTrue(payload.slotId())
                .orElseThrow(() -> new SlotNotFoundException("TimeSlot entity lookup missing for ID: " + payload.slotId()));

        // Finalize core database entity states
        slot.setStatus(SlotStatus.RESERVED);
        timeSlotRepository.save(slot);

        BookingEntity bookingEntity = new BookingEntity();
        bookingEntity.setTimeSlot(slot);
        bookingEntity.setTitle(payload.request().title());
        bookingEntity.setDescription(payload.request().description());
        bookingEntity.setParticipants(payload.request().participants());
        bookingRepository.save(bookingEntity);

        // Append final confirmation log to the Outbox table to alert downstream notification systems
        outboxEventService.saveOutbox(
                AggregateType.TIME_SLOT,
                String.valueOf(payload.slotId()),
                OutboxEventType.RESERVED_EVENT,
                payload
        );

        // Update the Redis operational state safely to RESERVED state
        final String slotKey = REDIS_KEY_PREFIX + payload.slotId();
        redisTemplate.execute(RESERVATION_SCRIPT, List.of(slotKey));
    }
}