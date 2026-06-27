package com.doodle.service;

import com.doodle.dto.LocalizedTimeSlotResponse;
import com.doodle.dto.TimeSlotResponse;
import com.doodle.enums.CustomSlotStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;

/**
 * Business orchestration contract managing timeline boundary queries,
 * regional time zone calendar translation, and DST gap auditing.
 */
public interface ICalendarService {

    /**
     * Resolves, filters, and translates chronological time segments for a specific user.
     * Adjusted dynamically into the client's preferred target time zone context.
     *
     * @param ownerId         Unique email identifier string of the calendar owner.
     * @param start           The absolute UTC start window boundary timestamp.
     * @param end             The absolute UTC end window boundary timestamp.
     * @param viewingTimeZone Target IANA time zone applied during calendar translation (e.g., "Asia/Karachi").
     * @param status          Filter criteria token restricting lookups to specific categories (FREE/RESERVED/ALL).
     * @param pageable        Pagination cursor and directional sorting layout properties.
     * @return A standard Page containing time-zone-shifted slot response tokens.
     */
    Page<LocalizedTimeSlotResponse> getAggregatedCalendar(
            String ownerId,
            Instant start,
            Instant end,
            String viewingTimeZone,
            CustomSlotStatus status,
            Pageable pageable
    );
}
