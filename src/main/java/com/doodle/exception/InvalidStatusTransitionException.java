package com.doodle.exception;

/**
 * Custom business exception thrown when an illegal state change is attempted
 * on a TimeSlot (e.g., trying to book a slot that is already RESERVED or NOT_AVAILABLE).
 */
public class InvalidStatusTransitionException extends RuntimeException {

    /**
     * Constructs a new status transition exception with an explicit failure cause.
     *
     * @param message The descriptive error detail mapping the disallowed state migration path.
     */
    public InvalidStatusTransitionException(String message) {
        super(message);
    }
}
