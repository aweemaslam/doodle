package com.doodle.service.impl;

import com.doodle.dto.BulkSlotRequest;
import com.doodle.dto.SlotRequest;
import com.doodle.dto.TimeSlotResponse;
import com.doodle.enums.SlotStatus;
import com.doodle.exception.ResourceNotFoundException;
import com.doodle.exception.SlotConflictException;
import com.doodle.exception.InvalidMethodArgumentException;
import com.doodle.model.TimeSlotEntity;
import com.doodle.model.UserEntity;
import com.doodle.repository.TimeSlotRepository;
import com.doodle.repository.UserRepository;
import com.doodle.service.TimeSlotService;
import com.doodle.service.helper.TimeSlotHelperService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Core business scheduling management processor.
 * Guarantees atomicity across relational database operations and enforces
 * transactional cache synchronization safety rules.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TimeSlotServiceImpl implements TimeSlotService {

    private final TimeSlotHelperService timeSlotHelperService;
    private final TimeSlotRepository timeSlotRepository;
    private final StringRedisTemplate redisTemplate;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public TimeSlotResponse createSlot(SlotRequest request) {
        timeSlotHelperService.validateTimezone(request.timezoneId());
        timeSlotHelperService.validateTimeBoundaries(request);

        UserEntity owner = fetchActiveUser(request.ownerId());
        ensureNoOverlap(owner.getEmail(), request.startTime(), request.endTime());

        TimeSlotEntity slot = new TimeSlotEntity();
        slot.setOwner(owner);
        slot.setStartTime(request.startTime());
        slot.setEndTime(request.endTime());
        slot.setTimezoneId(request.timezoneId());
        slot.setStatus(SlotStatus.FREE);

        TimeSlotEntity saved = timeSlotRepository.save(slot);

        // Sync to Redis ONLY after DB successfully logs transaction commit
        executePostCommit(() -> redisTemplate.opsForValue()
                .set(timeSlotHelperService.getRedisKey(saved.getId()), SlotStatus.FREE.name()));

        return timeSlotHelperService.mapToResponse(saved);
    }

    @Override
    @Transactional
    public List<TimeSlotResponse> createBulkSlots(BulkSlotRequest request) {
        timeSlotHelperService.validateTimezone(request.timezoneId());
        timeSlotHelperService.validateBulkRequest(request);

        UserEntity owner = fetchActiveUser(request.ownerId());
        ensureNoOverlap(owner.getEmail(), request.startTime(), request.endTime());

        long totalDurationMillis = Duration.between(request.startTime(), request.endTime()).toMillis();
        long slotDurationMillis = totalDurationMillis / request.numberOfSlots();

        if (slotDurationMillis <= 0) {
            throw new InvalidMethodArgumentException("The specified range is too narrow to divide into the requested number of slots.");
        }

        List<TimeSlotEntity> slotsToGenerate = new ArrayList<>();

        for (int i = 0; i < request.numberOfSlots(); i++) {
            Instant currentSlotStart = request.startTime().plusMillis(i * slotDurationMillis);
            Instant currentSlotEnd = (i == request.numberOfSlots() - 1)
                    ? request.endTime()
                    : currentSlotStart.plusMillis(slotDurationMillis);

            TimeSlotEntity slot = new TimeSlotEntity();
            slot.setOwner(owner);
            slot.setStartTime(currentSlotStart);
            slot.setEndTime(currentSlotEnd);
            slot.setTimezoneId(request.timezoneId());
            slot.setStatus(SlotStatus.FREE);

            slotsToGenerate.add(slot);
        }

        List<TimeSlotEntity> savedSlots = timeSlotRepository.saveAll(slotsToGenerate);

        // Batch seed Redis post-commit to minimize connection overhead
        Map<String, String> batchUpdates = savedSlots.stream()
                .collect(Collectors.toMap(
                        saved -> timeSlotHelperService.getRedisKey(saved.getId()),
                        saved -> SlotStatus.FREE.name()
                ));
        executePostCommit(() -> redisTemplate.opsForValue().multiSet(batchUpdates));

        return savedSlots.stream()
                .map(timeSlotHelperService::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public TimeSlotResponse modifySlot(Long id, SlotRequest request) {
        timeSlotHelperService.validateTimezone(request.timezoneId());
        timeSlotHelperService.validateTimeBoundaries(request);

        TimeSlotEntity slot = timeSlotRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Slot not found with ID: %d".formatted(id)));

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
        TimeSlotEntity slot = timeSlotRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Slot not found with ID: %d".formatted(id)));

        slot.setStatus(status);
        timeSlotRepository.save(slot);

        // Protect cache integrity using transactional synchronization callbacks
        executePostCommit(() -> redisTemplate.opsForValue()
                .set(timeSlotHelperService.getRedisKey(id), status.name()));
    }

    @Override
    @Transactional
    public void deleteSlot(Long id) {
        TimeSlotEntity slot = timeSlotRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Slot not found with ID: %d".formatted(id)));

        slot.setActive(false); // Clean soft deletion
        timeSlotRepository.save(slot);

        // Flush cache index only after database records confirm removal
        executePostCommit(() -> redisTemplate.delete(timeSlotHelperService.getRedisKey(id)));
    }

    private UserEntity fetchActiveUser(String email) {
        return userRepository.findByEmailAndActiveTrue(email)
                .orElseThrow(() -> new ResourceNotFoundException("Owner missing or deactivated with email: %s".formatted(email)));
    }

    private void ensureNoOverlap(String email, Instant start, Instant end) {
        if (timeSlotRepository.existsOverlappingSlot(email, start, end)) {
            throw new SlotConflictException("Requested time range conflicts with an existing slot partition allocation.");
        }
    }

    /**
     * Intercepts transaction lifecycles and safely defer executions to afterCommit phases.
     */
    private void executePostCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run(); // Fallback strategy for non-transactional paths
        }
    }
}