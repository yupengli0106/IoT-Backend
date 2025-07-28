package com.demo.myapp.exception;

import com.demo.myapp.controller.response.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.web.context.request.WebRequest;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author: Yupeng Li
 * @Date: 6/10/2024 19:36
 * @Description: IMPROVED - Global exception handler with structured logging and better error responses
 * 
 * IMPROVEMENTS:
 * - Added correlation ID support for request tracing
 * - Better error categorization and logging
 * - Secured error messages to prevent information disclosure
 * - Added database error handling
 * - Structured error response format
 */
@ControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * IMPROVED: Handle DeviceOperationException with better logging and error details
     */
    @ExceptionHandler(DeviceOperationException.class)
    public ResponseEntity<Result> handleDeviceOperationException(DeviceOperationException e, WebRequest request) {
        String correlationId = MDC.get("correlationId");
        logger.error("Device operation failed [{}]: {} - Request: {}", 
                    correlationId, e.getMessage(), request.getDescription(false), e);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.error(500, "Device operation failed: " + e.getMessage()));
    }

    /**
     * IMPROVED: Handle database exceptions with proper logging
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Result> handleDataAccessException(DataAccessException e, WebRequest request) {
        String correlationId = MDC.get("correlationId");
        logger.error("Database error [{}]: {} - Request: {}", 
                    correlationId, e.getMessage(), request.getDescription(false), e);
        
        // Don't expose database details to users
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.error(500, "A database error occurred. Please try again later."));
    }

    /**
     * IMPROVED: Handle generic exceptions with secure error messages
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result> handleGenericException(Exception e, WebRequest request) {
        String correlationId = MDC.get("correlationId");
        logger.error("Unexpected error [{}]: {} - Request: {}", 
                    correlationId, e.getMessage(), request.getDescription(false), e);
        
        // SECURITY: Don't expose internal error details to prevent information disclosure
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.error(500, "An unexpected error occurred. Please contact support with correlation ID: " + correlationId));
    }

    /**
     * IMPROVED: Handle validation exceptions with better logging
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result> handleValidationExceptions(MethodArgumentNotValidException ex, WebRequest request) {
        String correlationId = MDC.get("correlationId");
        
        assert ex.getBindingResult() != null;
        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.toList());

        logger.warn("Validation failed [{}]: {} - Request: {}", 
                   correlationId, errors, request.getDescription(false));

        return ResponseEntity.badRequest()
                .body(Result.error(400, "Validation failed: " + String.join(", ", errors)));
    }

    /**
     * IMPROVED: Handle authentication exceptions with secure logging
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Result> handleAuthenticationException(AuthenticationException ex, WebRequest request) {
        String correlationId = MDC.get("correlationId");
        
        // Log authentication failures for security monitoring (without exposing sensitive details)
        logger.warn("Authentication failed [{}] - Request: {} - Type: {}", 
                   correlationId, request.getDescription(false), ex.getClass().getSimpleName());
        
        // SECURITY: Use a generic message to prevent user enumeration attacks
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Result.error(401, "Authentication failed. Please check your credentials and try again."));
    }
    
    /**
     * IMPROVED: Handle user operation exceptions
     */
    @ExceptionHandler(UserOperationException.class)
    public ResponseEntity<Result> handleUserOperationException(UserOperationException e, WebRequest request) {
        String correlationId = MDC.get("correlationId");
        logger.error("User operation failed [{}]: {} - Request: {}", 
                    correlationId, e.getMessage(), request.getDescription(false), e);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.error(400, "User operation failed: " + e.getMessage()));
    }
}
