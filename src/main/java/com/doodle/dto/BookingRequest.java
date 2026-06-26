package com.doodle.dto;

import java.util.Set;

public record BookingRequest(String title, String description, Set<String> participants, String ownerId) {}
