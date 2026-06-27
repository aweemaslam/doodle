package com.doodle.controller;

import com.doodle.dto.ErrorResponse;
import com.doodle.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * Centralized API error mapping and global payload formatting.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler({MethodArgumentNotValidException.class, InvalidMethodArgumentException.class})
    public ResponseEntity<ErrorResponse> handleValidation(Exception ex) {
        String details;

        if (ex instanceof MethodArgumentNotValidException validationEx) {
            details = validationEx.getBindingResult().getFieldErrors().stream()
                    .map(error -> error.getField() + ": " + error.getDefaultMessage())
                    .distinct()
                    .collect(Collectors.joining("; "));

            if (details.isBlank()) {
                details = "Validation failed";
            }
        } else if (ex instanceof InvalidMethodArgumentException) {
            details = ex.getMessage();
        } else {
            details = "An unexpected validation error occurred.";
        }
        return build(HttpStatus.BAD_REQUEST, details);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail handleNotFoundError(NoResourceFoundException ex) {
        log.warn("API Endpoint fallback capture hit: {}", ex.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "The requested endpoint does not exist.");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMalformedJson(HttpMessageNotReadableException ex) {
        return build(HttpStatus.BAD_REQUEST, "Malformed JSON request body syntax or invalid type formats.");
    }

    @ExceptionHandler({SlotConflictException.class, InvalidStatusTransitionException.class})
    public ResponseEntity<ErrorResponse> handleGenericConflicts(Exception ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler({SlotNotFoundException.class, ResourceNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleNotFoundExceptions(Exception ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Unhandled internal server crash intercepted: ", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected internal server error occurred. Please contact support.");
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(status.value(), message));
    }
}
