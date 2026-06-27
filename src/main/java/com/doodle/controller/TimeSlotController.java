package com.doodle.controller;

import com.doodle.dto.BulkSlotRequest;
import com.doodle.dto.LocalizedTimeSlotResponse;
import com.doodle.dto.SlotRequest;
import com.doodle.dto.TimeSlotResponse;
import com.doodle.enums.CustomSlotStatus;
import com.doodle.enums.SlotStatus;
import com.doodle.service.ICalendarService;
import com.doodle.service.TimeSlotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/slots")
@RequiredArgsConstructor
@Tag(name = "Time Slot Management", description = "Endpoints for creating, modifying, deleting, and updating statuses of doodle scheduling slots.")
public class TimeSlotController {

    private final TimeSlotService timeSlotService;
    private final ICalendarService calendarService;

    @PostMapping
    @Operation(summary = "Create a new time slot", description = "Initializes a new time slot as FREE and syncs the initial state to the Redis cache.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Time slot successfully created",
                    content = @Content(schema = @Schema(implementation = TimeSlotResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid payload details supplied", content = @Content)
    })
    public ResponseEntity<TimeSlotResponse> createSlot(@Valid @RequestBody SlotRequest request) {
        TimeSlotResponse response = timeSlotService.createSlot(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/bulk")
    @Operation(summary = "Bulk generate successive time slots", description = "Divides a time frame into equal-length continuous availability windows")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "All continuous intervals generated Successfully.",
                    content = @Content(schema = @Schema(implementation = TimeSlotResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid payload constraints or indivisible time block supplied.", content = @Content),
            @ApiResponse(responseCode = "409", description = "One or more generated windows overlap with an existing slot.", content = @Content)
    })
    public ResponseEntity<List<TimeSlotResponse>> createBulkSlots(@Valid @RequestBody BulkSlotRequest request) {
        List<TimeSlotResponse> responses = timeSlotService.createBulkSlots(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modify an existing time slot", description = "Updates the start and end times for a specific slot ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Time slot modified successfully",
                    content = @Content(schema = @Schema(implementation = TimeSlotResponse.class))),
            @ApiResponse(responseCode = "404", description = "Time slot not found", content = @Content)
    })
    public ResponseEntity<TimeSlotResponse> modifySlot(
            @Parameter(description = "ID of the slot to update", required = true, example = "1") @PathVariable Long id,
            @Valid @RequestBody SlotRequest request) {
        TimeSlotResponse response = timeSlotService.modifySlot(id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Change time slot status", description = "Updates availability state (FREE/BUSY) of a slot and updates cached state in Redis.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Status updated successfully", content = @Content),
            @ApiResponse(responseCode = "404", description = "Time slot not found", content = @Content)
    })
    public ResponseEntity<Void> changeStatus(
            @Parameter(description = "ID of the slot to change status", required = true, example = "1") @PathVariable Long id,
            @Parameter(description = "New availability status", required = true, schema = @Schema(implementation = SlotStatus.class)) @RequestParam SlotStatus status) {
        timeSlotService.changeStatus(id, status);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an existing time slot", description = "Soft-deletes target slot from primary database and clears cached state from Redis.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Slot deleted successfully", content = @Content),
            @ApiResponse(responseCode = "404", description = "Time slot not found", content = @Content)
    })
    public ResponseEntity<Void> deleteSlot(
            @Parameter(description = "ID of the slot to delete", required = true, example = "1") @PathVariable Long id) {
        timeSlotService.deleteSlot(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{ownerId}")
    @Operation(summary = "Resolve compiled calendar availability views for a specific time range", description = "Queries database slot ranges adjusted dynamically into regional time zones.")
    @ApiResponse(responseCode = "200", description = "Aggregated chronological calendar mapping generated successfully.")
    public ResponseEntity<Page<LocalizedTimeSlotResponse>> getAggregatedCalendar(
                                                                         @Parameter(description = "Unique identifier email string of the calendar owner.") @PathVariable String ownerId,
                                                                         @Parameter(description = "The absolute UTC start window boundary timestamp.") @RequestParam Instant start,
                                                                         @Parameter(description = "The absolute UTC end window boundary timestamp.") @RequestParam Instant end,
                                                                         @Parameter(description = "Target viewing time zone context") @RequestParam(defaultValue = "UTC", required = false) String viewingTimeZone,
                                                                         @Parameter(description = "Slot status filter") @RequestParam(defaultValue = "ALL") CustomSlotStatus status,
                                                                         @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
                                                                         @Parameter(description = "Page size capacity limit") @RequestParam(defaultValue = "20") int size) {

        PageRequest pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(Sort.Direction.ASC, "startTime"));

        Page<LocalizedTimeSlotResponse> calendarPage = calendarService.getAggregatedCalendar(ownerId, start, end, viewingTimeZone, status, pageable);

        return ResponseEntity.ok(calendarPage);
    }
}