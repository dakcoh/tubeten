package com.tubeten.ten.config;

import com.tubeten.ten.exception.ErrorCode;
import com.tubeten.ten.exception.TubetenException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;

import java.io.IOException;

/**
 * YouTube API 호출 시 에러 응답 처리
 */
@Slf4j
public class YouTubeApiErrorHandler implements ResponseErrorHandler {
    
    @Override
    public boolean hasError(ClientHttpResponse response) throws IOException {
        HttpStatus statusCode = HttpStatus.resolve(response.getStatusCode().value());
        return statusCode != null && (statusCode.is4xxClientError() || statusCode.is5xxServerError());
    }
    
    @Override
    public void handleError(ClientHttpResponse response) throws IOException {
        HttpStatus statusCode = HttpStatus.resolve(response.getStatusCode().value());
        if (statusCode == null) {
            statusCode = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        String statusText = response.getStatusText();
        
        log.error("YouTube API 에러 응답 - status: {}, text: {}", statusCode, statusText);
        
        switch (statusCode) {
            case FORBIDDEN -> throw new TubetenException(ErrorCode.YOUTUBE_API_QUOTA_EXCEEDED, 
                    "YouTube API 할당량이 초과되었습니다: " + statusText);
            case BAD_REQUEST -> throw new TubetenException(ErrorCode.YOUTUBE_API_INVALID_RESPONSE, 
                    "잘못된 YouTube API 요청입니다: " + statusText);
            case TOO_MANY_REQUESTS -> throw new TubetenException(ErrorCode.YOUTUBE_API_QUOTA_EXCEEDED, 
                    "YouTube API 요청 한도를 초과했습니다: " + statusText);
            default -> {
                if (statusCode.is5xxServerError()) {
                    throw new TubetenException(ErrorCode.YOUTUBE_API_ERROR, 
                            "YouTube API 서버 오류: " + statusText);
                } else {
                    throw new TubetenException(ErrorCode.YOUTUBE_API_ERROR, 
                            "YouTube API 호출 실패: " + statusText);
                }
            }
        }
    }
}