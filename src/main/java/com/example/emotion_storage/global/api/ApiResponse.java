package com.example.emotion_storage.global.api;

import com.example.emotion_storage.global.exception.ErrorCode;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.time.LocalDateTime;

@JsonInclude(Include.NON_NULL)
public record ApiResponse<T>(
        int status,
        String code,
        String message,
        T data,
        LocalDateTime timestamp
) {
    private static final int SUCCESS_STATUS = 200;
    private static final String SUCCESS_CODE = "SUCCESS";
    private static final String SUCCESS_MESSAGE = "요청이 성공적으로 처리되었습니다.";

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(
                SUCCESS_STATUS,
                SUCCESS_CODE,
                SUCCESS_MESSAGE,
                data,
                LocalDateTime.now()
        );
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(
                SUCCESS_STATUS,
                SUCCESS_CODE,
                message,
                data,
                LocalDateTime.now()
        );
    }

    public static <T> ApiResponse<T> fail(ErrorCode errorCode) {
        return new ApiResponse<>(
                errorCode.getHttpStatus().value(),
                errorCode.name(),
                errorCode.getMessage(),
                null,
                LocalDateTime.now());
    }

    public static <T> ApiResponse<T> fail(ErrorCode errorCode, T data) {
        return new ApiResponse<>(
                errorCode.getHttpStatus().value(),
                errorCode.name(),
                errorCode.getMessage(),
                data,
                LocalDateTime.now());
    }
}
