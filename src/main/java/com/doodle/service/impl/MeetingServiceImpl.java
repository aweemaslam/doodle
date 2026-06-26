package com.doodle.service.impl;


import com.doodle.dto.BookingRequest;
import com.doodle.dto.OutboxEntityPayload;
import com.doodle.enums.AggregateType;
import com.doodle.enums.OutboxEventType;
import com.doodle.enums.SlotStatus;
import com.doodle.exception.InvalidStatusTransitionException;
import com.doodle.exception.ResourceNotFoundException;
import com.doodle.exception.SlotNotFoundException;
import com.doodle.model.MeetingEntity;
import com.doodle.model.TimeSlotEntity;
import com.doodle.repository.MeetingRepository;
import com.doodle.repository.TimeSlotRepository;
import com.doodle.service.IMeetingService;
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
public class MeetingServiceImpl implements IMeetingService {
    private final IOutboxEventService outboxEventService;
    private final StringRedisTemplate redisTemplate;
    private final MeetingRepository meetingRepository;
    private final TimeSlotRepository timeSlotRepository;

    private static final RedisScript<Long> PENDING_RESERVATION_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('GET', KEYS[1]) == 'FREE' then" +
                    "    redis.call('SET', KEYS[1], 'PENDING_RESERVATION')" +
                    "    return 1" +
                    "else" +
                    "    return 0" +
                    "end",
            Long.class
    );

    private static final RedisScript<Long> RESERVATION_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('GET', KEYS[1]) == 'PENDING_RESERVATION' then" +
                    "    redis.call('SET', KEYS[1], 'RESERVED')" +
                    "    return 1" +
                    "else" +
                    "    return 0" +
                    "end",
            Long.class
    );

    @Override
    @Transactional
    public boolean markPendingReservationInRedis(Long slotId, BookingRequest request) {
        final String slotKey = "slot:state:" + slotId;

        Long redisResult = redisTemplate.execute(
                PENDING_RESERVATION_SCRIPT,
                List.of(slotKey)
        );

        if (!Objects.equals(redisResult, 1L)) {
            return false;
        }

        outboxEventService.saveOutbox(AggregateType.TIME_SLOT, String.valueOf(slotId), OutboxEventType.PENDING_RESERVATION_EVENT, new OutboxEntityPayload(slotId, request));
        return true;
    }

    @Override
    @Transactional
    public void reserveSlotInRedis(Long slotId, BookingRequest request) {
        final String slotKey = "slot:state:" + slotId;

        Long redisResult = redisTemplate.execute(
                RESERVATION_SCRIPT,
                List.of(slotKey)
        );

        if (!Objects.equals(redisResult, 1L)) {
            throw new InvalidStatusTransitionException("Failed while Reserving the Slot.");
        }

        outboxEventService.saveOutbox(AggregateType.TIME_SLOT, String.valueOf(slotId), OutboxEventType.RESERVED_EVENT, new OutboxEntityPayload(slotId, request));
    }

    @Override
    @Transactional
    public void performReservation(OutboxEntityPayload payload) {
        // Idempotency check to avoid processing duplicate Kafka messages
        if (meetingRepository.existsByTimeSlotId(payload.slotId())) {
            return;
        }

        TimeSlotEntity slot = timeSlotRepository.findById(payload.slotId())
                .orElseThrow(() -> new SlotNotFoundException("Slot not found in DB for timeSlotId: " + payload.slotId()));

        // Persist status change to match memory layer
        slot.setStatus(SlotStatus.RESERVED);
        timeSlotRepository.save(slot);

        // Map meeting metadata attributes securely
        MeetingEntity meeting = new MeetingEntity();
        meeting.setTimeSlot(slot);
        meeting.setTitle(payload.request().title());
        meeting.setDescription(payload.request().description());
        meeting.setParticipants(payload.request().participants());
        meetingRepository.save(meeting);
        this.reserveSlotInRedis(payload.slotId(),payload.request());
    }
}