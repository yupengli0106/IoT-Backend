package com.demo.myapp.controller.response;

import lombok.Getter;

/**
 * @Author: Yupeng Li
 * @Date: 1/7/2024 15:21
 * @Description:
 */
public record Result(Integer code, String message, Object data) {
    public static Result success(Object data) {
        return new Result(200, "Success", data);
    }

    public static Result error(Integer code, String message) {
        return new Result(code, message, null);
    }

    public static Result error(ErrorEnum errorEnum) {
        return new Result(errorEnum.getCode(), errorEnum.getMessage(), null);
    }

    @Getter
    public enum ErrorEnum {
        // 定义错误码枚举
        INTERNAL_SERVER_ERROR(500, "Internal Server Error"),
        BAD_REQUEST(400, "Bad Request"),
        NOT_FOUND(404, "Not Found");

        private final Integer code;
        private final String message;

        ErrorEnum(Integer code, String message) {
            this.code = code;
            this.message = message;
        }

    }
}
