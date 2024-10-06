package com.demo.myapp.exception;

/**
 * @Author: Yupeng Li
 * @Date: 6/10/2024 20:31
 * @Description:
 */
public class UserOperationException extends RuntimeException {
    public UserOperationException(String message, Throwable cause) {
        super(message, cause);
    }

    public UserOperationException(String message) {
        super(message);
    }
}
