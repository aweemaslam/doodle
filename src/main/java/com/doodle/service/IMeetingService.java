package com.doodle.service;

import com.doodle.dto.BookingRequest;
import com.doodle.dto.OutboxEntityPayload;

public interface IMeetingService {
    boolean markPendingReservationInRedis(Long slotId, BookingRequest request);
    void reserveSlotInRedis(Long slotId, BookingRequest request);
    void performReservation(OutboxEntityPayload payload);
}
