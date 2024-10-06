package com.demo.myapp.exception;

/**
 * @Author: Yupeng Li
 * @Date: 6/10/2024 19:39
 * @Description:
 */
public class DeviceOperationException extends RuntimeException {

    public DeviceOperationException(String message) {
        super(message);
    }

    public DeviceOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
