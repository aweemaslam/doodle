package com.doodle.exception;

/**
 * Custom domain exception thrown when a data-concurrency or timing intersection occurs.
 * Typically triggered when a requested time segment overlaps with an already allocated slot layout.
 */
public class SlotConflictException extends RuntimeException {

    /**
     * Constructs a new slot conflict exception with an explicit failure cause.
     *
     * @param message The descriptive message detail mapping out the overlapping chronological collision bounds.
     */
    public SlotConflictException(String message) {
        super(message);
    }
}
