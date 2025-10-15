package com.tubeten.ten.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 에러 코드 정의
 */
@Getter
public enum ErrorCode {
    
    // YouTube API 관련 에러
    YOUTUBE_API_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "YOUTUBE_001", "YouTube API 호출 중 오류가 발생했습니다"),
    YOUTUBE_API_QUOTA_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "YOUTUBE_002", "YouTube API 할당량이 초과되었습니다"),
    YOUTUBE_API_INVALID_RESPONSE(HttpStatus.BAD_GATEWAY, "YOUTUBE_003", "YouTube API 응답이 올바르지 않습니다"),
    YOUTUBE_API_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "YOUTUBE_004", "YouTube API 호출 시간이 초과되었습니다"),
    
    // 캐시 관련 에러
    CACHE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "CACHE_001", "캐시 처리 중 오류가 발생했습니다"),
    CACHE_CONNECTION_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "CACHE_002", "캐시 서버 연결에 실패했습니다"),
    
    // 데이터베이스 관련 에러
    DATABASE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "DB_001", "데이터베이스 처리 중 오류가 발생했습니다"),
    DATA_NOT_FOUND(HttpStatus.NOT_FOUND, "DB_002", "요청한 데이터를 찾을 수 없습니다"),
    
    // 비즈니스 로직 에러
    INVALID_PARAMETER(HttpStatus.BAD_REQUEST, "BIZ_001", "잘못된 파라미터입니다"),
    INVALID_REGION_CODE(HttpStatus.BAD_REQUEST, "BIZ_002", "지원하지 않는 지역 코드입니다"),
    INVALID_CATEGORY_ID(HttpStatus.BAD_REQUEST, "BIZ_003", "잘못된 카테고리 ID입니다"),
    
    // 일반적인 에러
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "SYS_001", "내부 서버 오류가 발생했습니다"),
    EXTERNAL_SERVICE_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "SYS_002", "외부 서비스 연동 중 오류가 발생했습니다");
    
    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
    
    ErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}