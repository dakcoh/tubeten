package com.tubeten.ten.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * API 에러 응답 표준 포맷
 */
@Getter
@Builder
public class ErrorResponse {
    
    private final String code;
    private final String message;
    private final String detail;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private final LocalDateTime timestamp;
    
    public static ErrorResponse of(ErrorCode errorCode) {
        return ErrorResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    public static ErrorResponse of(ErrorCode errorCode, String detail) {
        return ErrorResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .detail(detail)
                .timestamp(LocalDateTime.now())
                .build();
    }
}