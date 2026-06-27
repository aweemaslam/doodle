package com.doodle.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * Standardized global error payload returned across all API endpoints when a transaction fails.
 */
@Schema(description = "Unified error payload structure containing execution diagnostic details.")
public record ErrorResponse(

        @Schema(description = "The standard HTTP semantic status code value.", example = "409")
        int status,

        @Schema(description = "Descriptive reason text explaining why the business operation failed.", example = "The requested time boundaries conflict with another configured slot.")
        String message,

        @Schema(description = "The precise absolute UTC timestamp when the exception occurred.")
        Instant timestamp
) {
    /**
     * Compact convenience constructor defaulting the execution timestamp context instantly to now.
     */
    public ErrorResponse(int status, String message) {
        this(status, message, Instant.now());
    }
}
