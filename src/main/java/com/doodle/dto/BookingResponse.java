package com.doodle.dto;

import com.doodle.enums.SlotStatus;

import java.util.Set;

public record BookingResponse(SlotStatus status, String message) {}
