package com.doodle.service.impl;

import com.doodle.dto.UserCalendar;
import com.doodle.model.TimeSlotEntity;
import com.doodle.repository.TimeSlotRepository;
import com.doodle.service.ICalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CalendarServiceImpl implements ICalendarService {
    private final TimeSlotRepository timeSlotRepository;

    @Override
    public List<TimeSlotEntity> getAggregatedCalendar(String ownerId, Instant start, Instant end, String viewingTimeZone) {
        List<TimeSlotEntity> rawSlots = timeSlotRepository
                .findByOwnerEmailAndStartTimeGreaterThanEqualAndEndTimeLessThanEqual(ownerId, start, end);

        UserCalendar calendar = new UserCalendar(ownerId, rawSlots);
        return calendar.getValidSlotsForZone(viewingTimeZone);
    }
}
