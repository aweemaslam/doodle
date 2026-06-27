package com.doodle.exception;

/**
 * Custom runtime exception thrown specifically when a targeted time slot entity
 * cannot be resolved or found within the active database schema.
 */
public class SlotNotFoundException extends RuntimeException {

    /**
     * Constructs a new slot lookup exception with a targeted descriptive error message.
     *
     * @param message The diagnostic text detail specifying the missing time slot identity bounds.
     */
    public SlotNotFoundException(String message) {
        super(message);
    }
}
