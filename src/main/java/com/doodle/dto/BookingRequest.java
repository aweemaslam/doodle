package com.doodle.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.util.Set;

/**
 * Clean data contract container capturing details needed to convert an open slot
 * into a confirmed meeting booking.
 */
public record BookingRequest(

        @NotBlank(message = "Meeting title cannot be blank or null.")
        @Size(max = 255, message = "Meeting title length cannot exceed 255 characters.")
        String title,

        @Size(max = 2000, message = "Description length cannot exceed 2000 characters.")
        String description,

        @NotEmpty(message = "A meeting requires at least 1 participant email.")
        Set<@Email(message = "Each participant must provide a valid email structure.") String> participants,

        @NotBlank(message = "Owner email identifier string is mandatory.")
        @Email(message = "Owner ID must adhere to a valid email format pattern.")
        String ownerId
) implements Serializable {
}
