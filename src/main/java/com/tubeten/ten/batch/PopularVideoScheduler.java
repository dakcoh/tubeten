package com.tubeten.ten.batch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tubeten.ten.api.dto.PopularVideoResponse;
import com.tubeten.ten.api.repository.VideoSnapshotRepository;
import com.tubeten.ten.api.service.PopularVideoService;
import com.tubeten.ten.domain.VideoSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PopularVideoScheduler {

    private final PopularVideoService popularVideoService;
    private final StringRedisTemplate redisTemplate;
    private final VideoSnapshotRepository videoSnapshotRepository;
    private final ObjectMapper objectMapper;

    @Value("${tubeten.scheduler.regions}")
    private List<String> regions;

    @Value("${tubeten.scheduler.categories:}")
    private List<String> categories;

    @Scheduled(cron = "0 0/30 * * * *")
    public void fetchAndCacheTop10() {
        log.info("\uD83D\uDD04 유튜브 Top10 수집 시작");

        for (String region : regions) {
            for (String categoryId : (categories.isEmpty() ? List.of((String) null) : categories)) {
                try {
                    List<PopularVideoResponse> top10 = popularVideoService.getPopularVideosRaw(region, categoryId);

                    // Redis 저장
                    String redisKey = buildRedisKey(region, categoryId);
                    String json = objectMapper.writeValueAsString(top10);
                    redisTemplate.opsForValue().set(redisKey, json, Duration.ofHours(1));
                    log.info("✅ Redis 저장 완료: {}", redisKey);

                    // DB 저장
                    LocalDateTime now = LocalDateTime.now();
                    int rank = 1;
                    for (PopularVideoResponse video : top10) {
                        VideoSnapshot snapshot = VideoSnapshot.builder()
                                .videoId(video.getVideoId())
                                .title(video.getTitle())
                                .channelTitle(video.getChannelTitle())
                                .rank(rank++)
                                .viewCount(video.getViewCount())
                                .regionCode(region)
                                .categoryId(categoryId)
                                .snapshotTime(now)
                                .build();
                        videoSnapshotRepository.save(snapshot);
                    }

                    log.info("✅ DB Snapshot 저장 완료 - region={}, category={}", region, categoryId);

                } catch (JsonProcessingException e) {
                    log.error("❌ JSON 변환 실패 - region={}, category={}", region, categoryId, e);
                } catch (Exception e) {
                    log.error("❌ 스케줄러 실행 실패 - region={}, category={}", region, categoryId, e);
                }
            }
        }

        log.info("\uD83C\uDF89 유튜브 Top10 수집 완료");
    }

    private String buildRedisKey(String region, String categoryId) {
        return "top10:" + region + (categoryId != null ? ":" + categoryId : ":all");
    }
}