package com.tubeten.ten.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tubeten.ten.api.dto.PopularVideoResponse;
import com.tubeten.ten.domain.PopularVideoWithTrend;
import com.tubeten.ten.api.repository.VideoSnapshotRepository;
import com.tubeten.ten.config.YoutubeApiConfig;
import com.tubeten.ten.domain.VideoSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
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

    private List<PopularVideoResponse> fetchYoutubeTop100(String regionCode, String categoryId) {
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
            return List.of();
        }

        JsonNode items = body.get("items");

        return StreamSupport.stream(items.spliterator(), false)
                .map(PopularVideoResponse::from)
                .toList();
    }

    private void cacheAndSaveSnapshot(String redisKey, List<PopularVideoResponse> videos, String region, String categoryId) {
        LocalDateTime now = LocalDateTime.now();

        List<VideoSnapshot> snapshots = new ArrayList<>();
        for (int i = 0; i < videos.size(); i++) {
            PopularVideoResponse v = videos.get(i);
            snapshots.add(VideoSnapshot.builder()
                    .videoId(v.getVideoId())
                    .title(v.getTitle())
                    .channelTitle(v.getChannelTitle())
                    .rank(i + 1)
                    .viewCount(v.getViewCount())
                    .regionCode(region)
                    .categoryId(categoryId)
                    .snapshotTime(now)
                    .build());
        }

        videoSnapshotRepository.saveAll(snapshots);
        log.info("✅ Snapshot 저장 완료: {}건 ({}, {})", snapshots.size(), region, categoryId);

        try {
            String json = objectMapper.writeValueAsString(videos);
            redisTemplate.opsForValue().set(redisKey, json, Duration.ofMinutes(30));
        } catch (JsonProcessingException e) {
            log.error("❌ Redis 저장 실패: {}", redisKey, e);
        }
    }

    public List<PopularVideoWithTrend> getPopularVideosWithAutoTrend(String regionCode, String categoryId) {
        String redisKey = "top100:" + regionCode + (categoryId != null ? ":" + categoryId : ":all");
        String cached = redisTemplate.opsForValue().get(redisKey);

        List<PopularVideoResponse> videos;

        if (cached != null) {
            log.info("✅ Redis 캐시 조회 성공: {}", redisKey);
            try {
                videos = objectMapper.readValue(cached, new TypeReference<>() {});
            } catch (JsonProcessingException e) {
                log.error("❌ Redis 파싱 실패 - fallback: {}", redisKey, e);
                videos = fetchYoutubeTop100(regionCode, categoryId);
            }
        } else {
            log.warn("❌ Redis MISS - YouTube API 호출: {}", redisKey);
            videos = fetchYoutubeTop100(regionCode, categoryId);
        }

        cacheAndSaveSnapshot(redisKey, videos, regionCode, categoryId);

        if (videoSnapshotRepository.existsByRegionCodeAndCategoryId(regionCode, categoryId)) {
            return popularVideoTrendService.getTopWithTrend(regionCode, categoryId, 100);
        } else {
            return videos.stream()
                    .map(video -> PopularVideoWithTrend.of(video, "new"))
                    .toList();
        }
    }

    public List<PopularVideoResponse> getPopularVideosRaw(String regionCode, String categoryId) {
        return fetchYoutubeTop100(regionCode, categoryId);
    }
}