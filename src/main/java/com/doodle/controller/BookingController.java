package com.doodle.controller;

import com.doodle.dto.BookingRequest;
import com.doodle.dto.BookingResponse;
import com.doodle.enums.SlotStatus;
import com.doodle.service.IBookingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/slots")
@RequiredArgsConstructor
@Tag(name = "Booking Scheduling Engine", description = "High-concurrency fast-path booking orchestration endpoints.")
public class BookingController {

    private final IBookingsService schedulingService;

    @PostMapping("/{id}/book")
    @Operation(
            summary = "Convert an available slot into a formalized booking",
            description = "Locks a target slot instantly using atomic Redis state check evaluations. If successful, an outbox message routes to the persistence pipeline to build the booking details asynchronously."
    )
    @ApiResponse(responseCode = "202", description = "Slot successfully locked. Permanent persistence is processing asynchronously.")
    @ApiResponse(responseCode = "409", description = "Concurrently rejected. The requested slot has already been claimed.")
    @ApiResponse(responseCode = "404", description = "Target time slot does not exist.")
    public ResponseEntity<BookingResponse> bookSlot(
            @Parameter(description = "Target Time Slot ID") @PathVariable Long id,
            @Valid @RequestBody BookingRequest request) {

        schedulingService.markPendingReservationInRedis(id, request);

        BookingResponse response = new BookingResponse(
                SlotStatus.PENDING_RESERVATION,
                "Slot reservation initiated successfully. Notification will follow processing completion."
        );

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
}