package com.doodle.controller;


import com.doodle.model.TimeSlotEntity;
import com.doodle.service.ICalendarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/calendars")
@RequiredArgsConstructor
@Tag(name = "Custom Calendar Aggregation", description = "Compiles and surfaces chronological availability tracking reports entirely inside the domain context layer.")
public class CalendarController {

    private final ICalendarService calendarService;
    @GetMapping("/{ownerId}")
    @Operation(
        summary = "Resolve compiled calendar availability views for a specific time range",
        description = "Queries historical database slot ranges and filters out invalid temporal entries (like skipped hours due to global Daylight Saving Time shifts) based on the viewing context zone."
    )
    @ApiResponse(responseCode = "200", description = "Aggregated chronological calendar mapping generated successfully.")
    public ResponseEntity<List<TimeSlotEntity>> getAggregatedCalendar(
            @Parameter(description = "Unique alphanumeric string identifying the calendar owner.") @PathVariable String ownerId,
            @Parameter(description = "The absolute UTC start window boundary timestamp.") @RequestParam Instant start,
            @Parameter(description = "The absolute UTC end window boundary timestamp.") @RequestParam Instant end,
            @Parameter(description = "Target IANA time zone used to filter DST anomalies for the viewer context.") @RequestParam(defaultValue = "UTC") String viewingTimeZone) {
        List<TimeSlotEntity> timeSlots = calendarService.getAggregatedCalendar(ownerId, start, end, viewingTimeZone);

        return ResponseEntity.ok(timeSlots);
    }
}