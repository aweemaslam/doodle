package com.doodle.service;

import com.doodle.dto.BulkSlotRequest;
import com.doodle.dto.SlotRequest;
import com.doodle.dto.TimeSlotResponse;
import com.doodle.enums.SlotStatus;

import java.util.List;

/**
 * Foundational domain lifecycle contract for Managing Time Slot Allocations.
 * Mandates validation boundaries, overlap protection checks, and batch generation mechanics.
 */
public interface TimeSlotService {

    /**
     * Allocates a singular availability segment for an active user.
     * Enforces localized chronological constraints and overlap checks before persistence.
     *
     * @param request The data payload containing timeline boundaries and timezone info.
     * @return A timezone-aligned presentation payload.
     */
    TimeSlotResponse createSlot(SlotRequest request);

    /**
     * Divides a continuous macro-timeline window into equal-length continuous partitions.
     * Evaluates collision constraints efficiently via a single unified range sweep.
     *
     * @param request The bulk generation criteria specifying interval count rules.
     * @return A list of all generated, timezone-aligned response profiles.
     */
    List<TimeSlotResponse> createBulkSlots(BulkSlotRequest request);

    /**
     * Modifies the timeframe or regional timezone settings of an existing slot.
     * Validates that updates do not collide with adjacent active user allocations.
     *
     * @param id      The target TimeSlot primary database auto-incrementing key.
     * @param request The updated chronological payload criteria.
     * @return The updated presentation model.
     */
    TimeSlotResponse modifySlot(Long id, SlotRequest request);

    /**
     * Transitions a slot cleanly across its Finite State Machine boundaries.
     *
     * @param id     The target TimeSlot primary database auto-incrementing key.
     * @param status The new status value (e.g., FREE, RESERVED, NOT_AVAILABLE).
     */
    void changeStatus(Long id, SlotStatus status);

    /**
     * Safely executes a soft-deletion routine to deactivate an availability window.
     *
     * @param id The target TimeSlot primary database auto-incrementing key.
     */
    void deleteSlot(Long id);
}