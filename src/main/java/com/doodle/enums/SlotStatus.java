package com.doodle.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Core domain enum representing the physical persistence availability states of a TimeSlot.
 * Locked strictly to actual database database state realities.
 */
@Schema(description = "The absolute physical state classification of an individual calendar time segment.")
public enum SlotStatus {
    /**
     * The slot is open, available for booking
     */
    @Schema(description = "The time slot is open and available for user selection.")
    FREE,

    /**
     * The slot has been scheduled for reservation
     */
    @Schema(description = "A reservation handshake has started; the slot is locked pending transactional confirmation.")
    PENDING_RESERVATION,

    /**
     * The slot is not available for booking
     */
    @Schema(description = "The slot has been blocked out or invalidated by the system administrator.")
    NOT_AVAILABLE,

    /**
     * The slot has been successfully claimed
     */
    @Schema(description = "The slot has been officially converted into a confirmed meeting booking.")
    RESERVED
}
