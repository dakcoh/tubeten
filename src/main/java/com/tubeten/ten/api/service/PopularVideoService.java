package com.tubeten.ten.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tubeten.ten.api.dto.PopularVideoResponse;
import com.tubeten.ten.api.dto.PopularVideoWithTrend;
import com.tubeten.ten.api.repository.VideoSnapshotRepository;
import com.tubeten.ten.config.YoutubeApiConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.stream.StreamSupport;

@Slf4j
@Service
@RequiredArgsConstructor
public class PopularVideoService {

    private final YoutubeApiConfig youtubeApiConfig;
    private final PopularVideoTrendService popularVideoTrendService;
    private final VideoSnapshotRepository videoSnapshotRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    // 공통 데이터 요청 로직
    private List<PopularVideoResponse> fetchYoutubeTop10(String regionCode, String categoryId) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUri(URI.create(youtubeApiConfig.getBaseUrl() + "/videos"))
                .queryParam("part", "snippet,statistics,contentDetails")
                .queryParam("chart", "mostPopular")
                .queryParam("regionCode", regionCode)
                .queryParam("maxResults", 100)
                .queryParam("key", youtubeApiConfig.getKey());

        if (categoryId != null && !categoryId.isBlank()) {
            builder.queryParam("videoCategoryId", categoryId);
        }

        String url = builder.toUriString();
        ResponseEntity<JsonNode> response = restTemplate.getForEntity(url, JsonNode.class);
        JsonNode body = response.getBody();
        if (body == null || body.get("items") == null) {
            log.warn("❌ YouTube API 응답에 items가 없습니다.");
            return List.of(); // 또는 예외 처리
        }
        JsonNode items = body.get("items");

        return StreamSupport.stream(items.spliterator(), false)
                .map(PopularVideoResponse::from)
                .toList();
    }

    // 스케줄러 등에서 순수 데이터만 필요할 때 사용
    public List<PopularVideoResponse> getPopularVideosRaw(String regionCode, String categoryId) {
        return fetchYoutubeTop10(regionCode, categoryId);
    }

    // 사용자 요청 시 트렌드 비교 자동 적용
    public List<PopularVideoWithTrend> getPopularVideosWithAutoTrend(String regionCode, String categoryId) {
        String redisKey = "top10:" + regionCode + (categoryId != null ? ":" + categoryId : ":all");
        String cached = redisTemplate.opsForValue().get(redisKey);

        if (cached != null) {
            log.info("✅ Redis 캐시에서 조회: {}", redisKey);
            try {
                List<PopularVideoResponse> videos = objectMapper.readValue(cached, new TypeReference<>() {});
                return popularVideoTrendService.addTrend(videos, regionCode, categoryId);
            } catch (JsonProcessingException e) {
                log.error("❌ Redis 캐시 파싱 오류 - fallback 실행: {}", redisKey, e);
            }
        }

        log.warn("❌ Redis MISS 또는 캐시 파싱 실패 → DB/YouTube fallback: {}", redisKey);

        boolean existsSnapshot = videoSnapshotRepository.existsByRegionCodeAndCategoryId(regionCode, categoryId);

        List<PopularVideoWithTrend> result;
        if (existsSnapshot) {
            result = popularVideoTrendService.getTop10WithTrend(regionCode, categoryId);
        } else {
            result = fetchYoutubeTop10(regionCode, categoryId).stream()
                    .map(video -> PopularVideoWithTrend.of(video, "new"))
                    .toList();
        }

        try {
            String json = objectMapper.writeValueAsString(result);
            redisTemplate.opsForValue().set(redisKey, json, Duration.ofMinutes(30));
        } catch (JsonProcessingException e) {
            log.error("❌ Redis 저장 실패: {}", redisKey, e);
        }

        return result;
    }
}
