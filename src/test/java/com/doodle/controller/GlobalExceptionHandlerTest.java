package com.doodle.controller;

import com.doodle.dto.ErrorResponse;
import com.doodle.exception.SlotConflictException;
import com.doodle.exception.InvalidStatusTransitionException;
import com.doodle.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    // ----------------------------
    // 1. SLOT CONFLICT EXCEPTION
    // ----------------------------
    @Test
    void shouldCreateSlotConflictException() {

        SlotConflictException ex = new SlotConflictException("Slot already booked");

        assertEquals("Slot already booked", ex.getMessage());
        assertNull(ex.getCause());
    }

    // ----------------------------
    // 2. INVALID STATUS TRANSITION EXCEPTION
    // ----------------------------
    @Test
    void shouldCreateInvalidStatusTransitionException() {

        InvalidStatusTransitionException ex = new InvalidStatusTransitionException("Cannot change from FREE to PENDING_RESERVATION");

        assertEquals("Cannot change from FREE to PENDING_RESERVATION", ex.getMessage());
        assertNotNull(ex);
    }

    // ----------------------------
    // 3. RESOURCE NOT FOUND EXCEPTION
    // ----------------------------
    @Test
    void shouldCreateResourceNotFoundException() {

        ResourceNotFoundException ex = new ResourceNotFoundException("Slot with id 123 not found");

        assertEquals("Slot with id 123 not found", ex.getMessage());
        assertNotNull(ex);
    }

    // ----------------------------
    // 4. EXCEPTION MESSAGE PROPAGATION
    // ----------------------------
    @Test
    void shouldPropagateExceptionMessage() {

        String message = "This is a test message";
        SlotConflictException ex = new SlotConflictException(message);

        assertTrue(ex.getMessage().contains("test message"));
    }

    // ----------------------------
    // 5. MULTIPLE EXCEPTION TYPES
    // ----------------------------
    @Test
    void shouldDifferentiateBetweenExceptionTypes() {

        Exception conflict = new SlotConflictException("Conflict");
        Exception invalid = new InvalidStatusTransitionException("Invalid");
        Exception notFound = new ResourceNotFoundException("Not found");

        assertNotSame(conflict.getClass(), invalid.getClass());
        assertNotSame(invalid.getClass(), notFound.getClass());
        assertNotSame(conflict.getClass(), notFound.getClass());
    }
}
