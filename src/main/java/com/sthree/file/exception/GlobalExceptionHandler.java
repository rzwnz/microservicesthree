package com.sthree.file.exception;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import com.sthree.file.dto.ApiResponse;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for the File Service.
 * 
 * Catches and handles all exceptions thrown by controllers,
 * converting them to appropriate HTTP responses with consistent
 * error format.
 * 
 * @author rzwnz
 * @version 1.0.0
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle FileServiceException and its subclasses.
     */
    @ExceptionHandler(FileServiceException.class)
    public ResponseEntity<ApiResponse<Void>> handleFileServiceException(FileServiceException ex) {
        log.error("File service error: {} - {}", ex.getErrorCode(), ex.getMessage());
        
        return ResponseEntity
                .status(ex.getStatusCode())
                .body(ApiResponse.error(ex.getMessage(), ex.getErrorCode()));
    }

    /**
     * Handle validation errors from @Valid annotations.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.warn("Validation error: {}", errors);

        return ResponseEntity
                .badRequest()
                .body(ApiResponse.<Map<String, String>>builder()
                        .success(false)
                        .message("Validation failed")
                        .errorCode(FileServiceException.ErrorCode.VALIDATION_ERROR)
                        .data(errors)
                        .build());
    }

    /**
     * Handle file size exceeded error.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxSizeException(MaxUploadSizeExceededException ex) {
        log.error("File size exceeded: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiResponse.error(
                        "File size exceeds maximum allowed upload size",
                        FileServiceException.ErrorCode.FILE_TOO_LARGE));
    }

    /**
     * Handle circuit breaker open state — storage service temporarily unavailable.
     */
    @ExceptionHandler(CallNotPermittedException.class)
    public ResponseEntity<ApiResponse<Void>> handleCircuitBreakerOpen(CallNotPermittedException ex) {
        log.warn("Circuit breaker open for {}: {}", ex.getCausingCircuitBreakerName(), ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error(
                        "Storage service is temporarily unavailable. Please try again later.",
                        FileServiceException.ErrorCode.INTERNAL_ERROR));
    }

    /**
     * Handle all other unexpected exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred", ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(
                        "An unexpected error occurred",
                        FileServiceException.ErrorCode.INTERNAL_ERROR));
    }
}
