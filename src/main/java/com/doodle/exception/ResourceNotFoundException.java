package com.doodle.exception;

/**
 * Universal data-access runtime exception thrown when a requested persistence entity
 * (such as a specific User or unexpected internal resource profile) cannot be resolved.
 */
public class ResourceNotFoundException extends RuntimeException {

    /**
     * Constructs a new lookup exception with a dynamic descriptive failure string.
     *
     * @param message The formatted diagnostic text specifying which database lookup failed.
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
