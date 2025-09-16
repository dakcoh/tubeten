package com.tubeten.ten.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tubeten.ten.api.dto.PopularVideoResponse;
import com.tubeten.ten.api.repository.VideoSnapshotRepository;
import com.tubeten.ten.config.YoutubeApiConfig;
import com.tubeten.ten.domain.PopularVideoWithTrend;
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
import java.util.*;

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

    // 규칙: region = 대문자, categoryId = null 이면 all
    private static String normRegion(String region) {
        return region == null ? "KR" : region.toUpperCase();
    }
    private static String normCat(String cat) {
        return (cat == null || cat.isBlank()) ? null : cat.trim();
    }
    private static String key(String region, String categoryId) {
        return "top100:" + region + (categoryId != null ? ":" + categoryId : ":all");
    }

    private List<PopularVideoResponse> fetchYoutubeTop100(String region, String categoryId) {
        String r = normRegion(region);
        String c = normCat(categoryId);
        List<PopularVideoResponse> out = new ArrayList<>(100);

        String pageToken = null;
        for (int page = 0; page < 2; page++) {
            UriComponentsBuilder b = UriComponentsBuilder
                    .fromUri(URI.create(youtubeApiConfig.getBaseUrl() + "/videos"))
                    .queryParam("part", "snippet,statistics,contentDetails")
                    .queryParam("chart", "mostPopular")
                    .queryParam("regionCode", r)
                    .queryParam("maxResults", 50)
                    .queryParam("key", youtubeApiConfig.getKey());
            if (c != null) b.queryParam("videoCategoryId", c);
            if (pageToken != null) b.queryParam("pageToken", pageToken);

            ResponseEntity<JsonNode> res = restTemplate.getForEntity(b.toUriString(), JsonNode.class);
            JsonNode body = res.getBody();
            if (body == null || body.get("items") == null) break;

            for (JsonNode item : body.get("items")) out.add(PopularVideoResponse.from(item));
            pageToken = body.has("nextPageToken") ? body.get("nextPageToken").asText(null) : null;
            if (pageToken == null) break;
        }
        return out.size() > 100 ? out.subList(0, 100) : out;
    }

    private void cacheAndSaveSnapshot(String redisKey, List<PopularVideoResponse> videos, String region, String categoryId) {
        String r = normRegion(region);
        String c = normCat(categoryId);
        LocalDateTime now = LocalDateTime.now();

        if (!videos.isEmpty()) {
            List<VideoSnapshot> snapshots = new ArrayList<>(videos.size());
            for (int i = 0; i < videos.size(); i++) {
                PopularVideoResponse v = videos.get(i);
                snapshots.add(VideoSnapshot.builder()
                        .videoId(v.videoId())
                        .title(v.title())
                        .channelTitle(v.channelTitle())
                        .rank(i + 1)
                        .viewCount(v.viewCount())
                        .regionCode(r)
                        .categoryId(c)
                        .snapshotTime(now)
                        .build());
            }
            videoSnapshotRepository.saveAll(snapshots);
            log.info("✅ Snapshot 저장 완료: {}건 ({}, {})", snapshots.size(), r, c);
        } else {
            log.info("ℹ️ Snapshot 생략: 빈 목록 ({}, {})", r, c);
        }

        try {
            String json = objectMapper.writeValueAsString(videos);
            redisTemplate.opsForValue().set(redisKey, json, Duration.ofMinutes(30));
        } catch (JsonProcessingException e) {
            log.error("❌ Redis 저장 실패: {}", redisKey, e);
        }
    }

    public List<PopularVideoWithTrend> getPopularVideosWithAutoTrend(String regionCode, String categoryId, int offset, int limit) {
        String r = normRegion(regionCode);
        String c = normCat(categoryId);
        String redisKey = key(r, c);

        List<PopularVideoResponse> videos;
        String cached = redisTemplate.opsForValue().get(redisKey);

        if (cached != null) {
            log.info("✅ Redis 캐시 조회 성공: {}", redisKey);
            try {
                videos = objectMapper.readValue(cached, new TypeReference<>() {});
            } catch (Exception e) {
                log.error("❌ Redis 파싱 실패 - fallback: {}", redisKey, e);
                // 불량 캐시 제거 후 재생성
                redisTemplate.delete(redisKey);
                videos = fetchYoutubeTop100(r, c);
                cacheAndSaveSnapshot(redisKey, videos, r, c);
            }
        } else {
            log.warn("❌ Redis MISS - YouTube API 호출: {}", redisKey);
            videos = fetchYoutubeTop100(r, c);
            cacheAndSaveSnapshot(redisKey, videos, r, c);
        }

        if (videos.isEmpty()) return List.of();

        int toIndex = Math.min(offset + limit, videos.size());
        if (offset >= toIndex) return List.of();

        // 트렌드가 있으면 트렌드 기준으로 페이징 반환
        if (videoSnapshotRepository.existsByRegionCodeAndCategoryId(r, c)) {
            List<PopularVideoWithTrend> full = popularVideoTrendService.getTopWithTrend(r, c, 100);
            return full.subList(offset, toIndex);
        }

        // 초기 스냅샷만 있고 비교 기준 없음 → new
        List<PopularVideoResponse> page = videos.subList(offset, toIndex);
        return page.stream().map(v -> PopularVideoWithTrend.of(v, "new")).toList();
    }

    public List<PopularVideoResponse> getPopularVideosRaw(String regionCode, String categoryId) {
        return fetchYoutubeTop100(regionCode, categoryId);
    }
}
