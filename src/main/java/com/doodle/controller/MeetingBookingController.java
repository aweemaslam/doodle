package com.doodle.controller;


import com.doodle.dto.BookingRequest;
import com.doodle.dto.BookingResponse;
import com.doodle.enums.SlotStatus;
import com.doodle.service.IMeetingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/slots")
@RequiredArgsConstructor
@Tag(name = "Meeting Scheduling Engine", description = "High-concurrency fast-path booking engine. Interacts directly with transient in-memory validation components.")
public class MeetingBookingController {

    private final IMeetingService schedulingService;

    @PostMapping("/{id}/book")
    @Operation(summary = "Convert an available slot into a formalized meeting", description = "Locks a target slot instantly using atomic state check evaluations. If successful, an outbox message routes to the persistence pipeline to build the meeting details asynchronously.")
    @ApiResponse(responseCode = "202", description = "Slot successfully locked in memory. Permanent persistence is under processing.")
    @ApiResponse(responseCode = "409", description = "Concurrently rejected. The requested slot is already marked BUSY or claimed by another user connection.")
    public ResponseEntity<BookingResponse> bookMeeting(@Parameter(description = "The target persistent ID of the time slot being converted.") @PathVariable Long id, @RequestBody BookingRequest request) {

        boolean processOk = schedulingService.markPendingReservationInRedis(id, request);

        if (processOk) {
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(new BookingResponse(SlotStatus.PENDING_RESERVATION, "Slot is pending reservation, once confirmed will receive notification in a while."));
        }

        return ResponseEntity.status(HttpStatus.CONFLICT).body(new BookingResponse(SlotStatus.NOT_AVAILABLE, "Slot not available or already reserved"));
    }
}