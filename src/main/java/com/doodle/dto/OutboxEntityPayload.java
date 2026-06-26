package com.doodle.dto;

import java.io.Serializable;

public record OutboxEntityPayload(
        Long slotId, BookingRequest request) implements Serializable {
}

