package com.tubeten.ten.exception;

import lombok.Getter;

/**
 * TubeTen 애플리케이션의 기본 예외 클래스
 */
@Getter
public class TubetenException extends RuntimeException {
    
    private final ErrorCode errorCode;
    
    public TubetenException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
    
    public TubetenException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public TubetenException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }
    
    public TubetenException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}