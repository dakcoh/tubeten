package com.tubeten.ten.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate 설정
 */
@Slf4j
@Configuration
public class RestTemplateConfig {
    
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout(15000); // 15초 (운영 환경에서 더 여유롭게)
        factory.setConnectionRequestTimeout(15000); // 15초
        factory.setReadTimeout(30000); // 30초 (YouTube API 응답 대기)
        
        return builder
                .requestFactory(() -> factory)
                .errorHandler(new YouTubeApiErrorHandler())
                .build();
    }
}