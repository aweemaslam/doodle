package com.doodle.service.impl;

import com.doodle.dto.LocalizedTimeSlotResponse;
import com.doodle.dto.UserCalendar;
import com.doodle.enums.CustomSlotStatus;
import com.doodle.enums.SlotStatus;
import com.doodle.exception.ResourceNotFoundException;
import com.doodle.model.TimeSlotEntity;
import com.doodle.model.UserEntity;
import com.doodle.repository.TimeSlotRepository;
import com.doodle.repository.UserRepository;
import com.doodle.service.ICalendarService;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.util.Strings;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CalendarServiceImpl implements ICalendarService {

    private final TimeSlotRepository timeSlotRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<LocalizedTimeSlotResponse> getAggregatedCalendar(String ownerId, Instant start, Instant end, String viewingTimeZone, CustomSlotStatus status, Pageable pageable) {
        UserEntity owner = userRepository.findByEmailAndActiveTrue(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Owner Not Found With Id %s".formatted(ownerId)));

        if (Strings.isBlank(viewingTimeZone)) {
            viewingTimeZone = owner.getDefaultTimezone();
        }
        // Fetch paged records from database
        Page<TimeSlotEntity> entityPage = fetchSlotsFromRepository(ownerId, start, end, status, pageable);

        // Wrap into domain record to filter DST anomalies and map to response models
        UserCalendar calendar = new UserCalendar(ownerId, entityPage.getContent());
        List<LocalizedTimeSlotResponse> customizedResponses = calendar.getValidLocalizedSlots(viewingTimeZone);

        // Return a clean page mapping using original metadata
        return new PageImpl<>(customizedResponses, pageable, entityPage.getTotalElements());
    }

    private Page<TimeSlotEntity> fetchSlotsFromRepository(String ownerId, Instant start, Instant end, CustomSlotStatus status, Pageable pageable) {
        if (status == null || status == CustomSlotStatus.ALL) {
            return timeSlotRepository.findByOwnerEmailAndStartTimeGreaterThanEqualAndEndTimeLessThanEqualAndActiveTrue(
                    ownerId, start, end, pageable
            );
        }


        SlotStatus domainStatus = SlotStatus.valueOf(status.name());
        return timeSlotRepository.findByOwnerEmailAndStartTimeGreaterThanEqualAndEndTimeLessThanEqualAndStatusAndActiveTrue(
                ownerId, start, end, domainStatus, pageable
        );
    }
}
