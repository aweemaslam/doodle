package com.doodle.enums;

/**
 * Represents the availability state of a given TimeSlot.
 */
public enum SlotStatus {
    /**
     * The slot is open, available for booking, and can be converted into a meeting.
     */
    FREE,

    /**
     * The slot has been scheduled for reservation
     */
    PENDING_RESERVATION,

    /**
     * The slot is not available for booking
     */
    NOT_AVAILABLE,
    /**
     * The slot has been successfully claimed
     */
    RESERVED
}