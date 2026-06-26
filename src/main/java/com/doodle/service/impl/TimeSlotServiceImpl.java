package com.doodle.service.impl;

import com.doodle.dto.BulkSlotRequest;
import com.doodle.dto.SlotRequest;
import com.doodle.dto.TimeSlotResponse;
import com.doodle.enums.SlotStatus;
import com.doodle.exception.ResourceNotFoundException;
import com.doodle.exception.SlotConflictException;
import com.doodle.model.TimeSlotEntity;
import com.doodle.model.UserEntity;
import com.doodle.repository.TimeSlotRepository;
import com.doodle.repository.UserRepository;
import com.doodle.service.TimeSlotService;
import com.doodle.service.helper.TimeSlotHelperService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class TimeSlotServiceImpl implements TimeSlotService {
    private final TimeSlotHelperService timeSlotHelperService;
    private final TimeSlotRepository timeSlotRepository;
    private final StringRedisTemplate redisTemplate;
    private final UserRepository userRepository;

    private static final String REDIS_KEY_PREFIX = "slot:state:";

    @Override
    @Transactional
    public TimeSlotResponse createSlot(SlotRequest request) {
        timeSlotHelperService.validateTimezone(request.timezoneId());
        timeSlotHelperService.validateTimeBoundaries(request);

        UserEntity owner = userRepository.findById(request.ownerId())
                .orElseThrow(() -> new ResourceNotFoundException("Owner Not Found With Id %s".formatted(request.ownerId())));

        // Edge Case: Prevent overlapping slots for the same owner
        boolean isOverlapping = timeSlotRepository.existsOverlappingSlot(
                owner.getEmail(), request.startTime(), request.endTime()
        );
        if (isOverlapping) {
            throw new SlotConflictException("Requested time frame overlaps with an existing slot for this user.");
        }

        TimeSlotEntity slot = new TimeSlotEntity();
        slot.setOwner(owner);
        slot.setStartTime(request.startTime());
        slot.setEndTime(request.endTime());
        slot.setTimezoneId(request.timezoneId());
        slot.setStatus(SlotStatus.FREE);

        TimeSlotEntity saved = timeSlotRepository.save(slot);
        redisTemplate.opsForValue().set(timeSlotHelperService.getRedisKey(saved.getId()), SlotStatus.FREE.name());

        return timeSlotHelperService.mapToResponse(saved);
    }

    @Override
    @Transactional
    public List<TimeSlotResponse> createBulkSlots(BulkSlotRequest request) {
        // 1. Structural Validation Guards
        timeSlotHelperService.validateTimezone(request.timezoneId());
        timeSlotHelperService.validateBulkRequest(request);

        UserEntity owner = userRepository.findById(request.ownerId())
                .orElseThrow(() -> new ResourceNotFoundException("Owner Not Found With Id %s".formatted(request.ownerId())));

        // 2. Compute Segment Durations
        long totalDurationMillis = java.time.Duration.between(request.startTime(), request.endTime()).toMillis();
        long slotDurationMillis = totalDurationMillis / request.numberOfSlots();

        if (slotDurationMillis <= 0) {
            throw new IllegalArgumentException("The specified range is too narrow to divide into the requested number of slots.");
        }

        List<TimeSlotEntity> slotsToGenerate = new ArrayList<>();

        // 3. Interval Building & Collision Evaluation Loop
        for (int i = 0; i < request.numberOfSlots(); i++) {
            Instant currentSlotStart = request.startTime().plusMillis(i * slotDurationMillis);

            // Edge Case: Handing precision truncation remainders on the concluding element
            Instant currentSlotEnd = (i == request.numberOfSlots() - 1)
                    ? request.endTime()
                    : currentSlotStart.plusMillis(slotDurationMillis);

            // Check if this generated window hits an overlap conflict
            boolean isOverlapping = timeSlotRepository.existsOverlappingSlot(
                    owner.getEmail(), currentSlotStart, currentSlotEnd
            );
            if (isOverlapping) {
                throw new SlotConflictException(
                        "Bulk generation aborted. The interval segment %d [%s - %s] collides with an existing allocation configuration."
                                .formatted(i + 1, currentSlotStart, currentSlotEnd)
                );
            }

            TimeSlotEntity slot = new TimeSlotEntity();
            slot.setOwner(owner);
            slot.setStartTime(currentSlotStart);
            slot.setEndTime(currentSlotEnd);
            slot.setTimezoneId(request.timezoneId());
            slot.setStatus(SlotStatus.FREE);

            slotsToGenerate.add(slot);
        }

        // 4. Batch Persist & Cache Seeding
        List<TimeSlotEntity> savedEntities = timeSlotRepository.saveAll(slotsToGenerate);

        for (TimeSlotEntity saved : savedEntities) {
            redisTemplate.opsForValue().set(timeSlotHelperService.getRedisKey(saved.getId()), SlotStatus.FREE.name());
        }

        return savedEntities.stream()
                .map(timeSlotHelperService::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public TimeSlotResponse modifySlot(Long id, SlotRequest request) {
        timeSlotHelperService.validateTimezone(request.timezoneId());
        timeSlotHelperService.validateTimeBoundaries(request);

        TimeSlotEntity slot = timeSlotRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Slot not found with ID: %d".formatted(id)));

        // Edge Case: Prevent modifying a slot that has already been booked (Status != FREE)
        if (!Objects.equals(slot.getStatus(), SlotStatus.FREE)) {
            throw new SlotConflictException("Cannot modify time boundaries. This slot is already booked for an active meeting.");
        }

        // Edge Case: Prevent modifications that collide with other existing windows
        boolean isOverlapping = timeSlotRepository.existsOverlappingSlotExcludingId(
                slot.getOwner().getEmail(), request.startTime(), request.endTime(), id
        );
        if (isOverlapping) {
            throw new SlotConflictException("The updated time boundaries conflict with another configured slot.");
        }

        slot.setStartTime(request.startTime());
        slot.setEndTime(request.endTime());
        slot.setTimezoneId(request.timezoneId());

        TimeSlotEntity updated = timeSlotRepository.save(slot);
        return timeSlotHelperService.mapToResponse(updated);
    }

    @Override
    @Transactional
    public void changeStatus(Long id, SlotStatus status) {
        TimeSlotEntity slot = timeSlotRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Slot not found with ID: %d".formatted(id)));

        slot.setStatus(status);
        timeSlotRepository.save(slot);

        redisTemplate.opsForValue().set(timeSlotHelperService.getRedisKey(id), status.name());
    }

    @Override
    @Transactional
    public void deleteSlot(Long id) {
        if (!timeSlotRepository.existsById(id)) {
            throw new ResourceNotFoundException("Slot not found with ID: %d".formatted(id));
        }
        timeSlotRepository.deleteById(id);
        redisTemplate.delete(timeSlotHelperService.getRedisKey(id));
    }
}