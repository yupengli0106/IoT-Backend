package com.demo.myapp.exception;

import com.demo.myapp.controller.response.Result;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.context.support.DefaultMessageSourceResolvable;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author: Yupeng Li
 * @Date: 6/10/2024 19:36
 * @Description:
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle DeviceOperationException exception
     * @param e DeviceOperationException
     * @return ResponseEntity
     */
    @ExceptionHandler(DeviceOperationException.class)
    public ResponseEntity<Result> handleDeviceOperationException(DeviceOperationException e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Result.error(500, e.getMessage()));
    }

    /**
     * Handle Exception exception
     * @param e Exception
     * @return ResponseEntity
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result> handleGenericException(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Result.error(500, "Internal Server Error"));
    }

    /**
     * Handle MethodArgumentNotValidException exception
     * @param ex MethodArgumentNotValidException
     * @return ResponseEntity
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result> handleValidationExceptions(MethodArgumentNotValidException ex) {
        assert ex.getBindingResult() != null;
        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.toList());

        return ResponseEntity.badRequest().body(Result.error(400, String.join(", ", errors)));
    }

    /**
     * Handle AuthenticationException exception
     * @param ex AuthenticationException
     * @return ResponseEntity
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Result> handleAuthenticationException(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Result.error(401, "Invalid username or password, please try again"));
    }
}
