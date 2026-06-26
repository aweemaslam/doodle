package com.doodle.service;

import com.doodle.model.TimeSlotEntity;

import java.time.Instant;
import java.util.List;

public interface ICalendarService {
    public List<TimeSlotEntity> getAggregatedCalendar(String ownerId, Instant start, Instant end, String viewingTimeZone);
}
