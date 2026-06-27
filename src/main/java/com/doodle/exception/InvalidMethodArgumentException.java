package com.doodle.exception;

/**
 * Custom runtime exception thrown when manual business rule validations fail.
 * Managed centrally by the GlobalExceptionHandler to avoid cross-cutting configuration bloat.
 */
public class InvalidMethodArgumentException extends RuntimeException {

    /**
     * Constructs a new validation exception with a targeted descriptive failure cause.
     *
     * @param message The diagnostic reason text detailing the exact validation rule violation.
     */
    public InvalidMethodArgumentException(String message) {
        super(message);
    }
}
