package com.hotelmanagement.quanlikhachsan.exception;

import com.hotelmanagement.quanlikhachsan.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for the application.
 * Provides consistent error responses across all controllers.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handle AppError exceptions (business logic errors).
     */
    @ExceptionHandler(AppError.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleAppError(AppError ex) {
        log.error("AppError occurred: {} - {}", ex.getErrorCode(), ex.getMessage());

        Map<String, Object> errorDetails = new HashMap<>(ex.getDetails());
        errorDetails.put("errorCode", ex.getErrorCode());

        ApiResponse<Map<String, Object>> response = ApiResponse.error(errorDetails, ex.getMessage());
        return ResponseEntity.status(ex.getStatusCode()).body(response);
    }

    /**
     * Handle validation errors from @Valid annotations.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        log.error("Validation error occurred");

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ApiResponse<Map<String, String>> response = ApiResponse.error(errors, "Validation failed");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handle ResourceNotFoundException.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleResourceNotFound(ResourceNotFoundException ex) {
        log.error("Resource not found: {}", ex.getMessage());

        Map<String, Object> details = new HashMap<>();
        details.put("resource", ex.getResourceName());
        details.put("field", ex.getFieldName());
        details.put("value", ex.getFieldValue());

        ApiResponse<Map<String, Object>> response = ApiResponse.error(details, ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Handle all other uncaught exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<String>> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred", ex);

        ApiResponse<String> response = ApiResponse.error("An unexpected error occurred. Please try again later.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
