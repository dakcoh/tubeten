package com.tubeten.ten.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 전역 예외 처리기
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    /**
     * TubeTen 커스텀 예외 처리
     */
    @ExceptionHandler(TubetenException.class)
    public ResponseEntity<ErrorResponse> handleTubetenException(TubetenException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.error("TubetenException occurred: code={}, message={}", errorCode.getCode(), e.getMessage(), e);
        
        ErrorResponse errorResponse = ErrorResponse.of(errorCode, e.getMessage());
        return ResponseEntity.status(errorCode.getHttpStatus()).body(errorResponse);
    }
    
    /**
     * HTTP 클라이언트 에러 (4xx) 처리
     */
    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity<ErrorResponse> handleHttpClientErrorException(HttpClientErrorException e) {
        log.error("HTTP Client Error: status={}, message={}", e.getStatusCode(), e.getMessage(), e);
        
        ErrorCode errorCode = determineYouTubeErrorCode(e);
        ErrorResponse errorResponse = ErrorResponse.of(errorCode, "외부 API 호출 중 클라이언트 오류: " + e.getMessage());
        return ResponseEntity.status(errorCode.getHttpStatus()).body(errorResponse);
    }
    
    /**
     * HTTP 서버 에러 (5xx) 처리
     */
    @ExceptionHandler(HttpServerErrorException.class)
    public ResponseEntity<ErrorResponse> handleHttpServerErrorException(HttpServerErrorException e) {
        log.error("HTTP Server Error: status={}, message={}", e.getStatusCode(), e.getMessage(), e);
        
        ErrorResponse errorResponse = ErrorResponse.of(ErrorCode.YOUTUBE_API_ERROR, 
                "외부 API 서버 오류: " + e.getMessage());
        return ResponseEntity.status(ErrorCode.YOUTUBE_API_ERROR.getHttpStatus()).body(errorResponse);
    }
    
    /**
     * 네트워크 연결 에러 처리
     */
    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<ErrorResponse> handleResourceAccessException(ResourceAccessException e) {
        log.error("Resource Access Error: message={}", e.getMessage(), e);
        
        ErrorCode errorCode = e.getMessage() != null && e.getMessage().contains("timeout") 
                ? ErrorCode.YOUTUBE_API_TIMEOUT 
                : ErrorCode.EXTERNAL_SERVICE_ERROR;
                
        ErrorResponse errorResponse = ErrorResponse.of(errorCode, e.getMessage());
        return ResponseEntity.status(errorCode.getHttpStatus()).body(errorResponse);
    }
    
    /**
     * 데이터베이스 접근 에러 처리
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDataAccessException(DataAccessException e) {
        log.error("Database Access Error: message={}", e.getMessage(), e);
        
        ErrorResponse errorResponse = ErrorResponse.of(ErrorCode.DATABASE_ERROR, e.getMessage());
        return ResponseEntity.status(ErrorCode.DATABASE_ERROR.getHttpStatus()).body(errorResponse);
    }
    
    /**
     * 파라미터 타입 불일치 에러 처리
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.error("Method Argument Type Mismatch: parameter={}, value={}", e.getName(), e.getValue(), e);
        
        String detail = String.format("파라미터 '%s'의 값 '%s'이(가) 올바르지 않습니다", e.getName(), e.getValue());
        ErrorResponse errorResponse = ErrorResponse.of(ErrorCode.INVALID_PARAMETER, detail);
        return ResponseEntity.status(ErrorCode.INVALID_PARAMETER.getHttpStatus()).body(errorResponse);
    }
    
    /**
     * 일반적인 예외 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Unexpected error occurred: message={}", e.getMessage(), e);
        
        ErrorResponse errorResponse = ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR);
        return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus()).body(errorResponse);
    }
    
    /**
     * YouTube API 에러 코드 결정
     */
    private ErrorCode determineYouTubeErrorCode(HttpClientErrorException e) {
        return switch (e.getStatusCode().value()) {
            case 403 -> ErrorCode.YOUTUBE_API_QUOTA_EXCEEDED;
            case 400 -> ErrorCode.YOUTUBE_API_INVALID_RESPONSE;
            default -> ErrorCode.YOUTUBE_API_ERROR;
        };
    }
}