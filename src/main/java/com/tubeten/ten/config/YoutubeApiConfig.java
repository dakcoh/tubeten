package com.tubeten.ten.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "youtube.api")
@Data
public class YoutubeApiConfig {
    private String key;
    private String baseUrl;
}