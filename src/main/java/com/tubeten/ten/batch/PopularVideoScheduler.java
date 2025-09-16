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
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PopularVideoScheduler {

    private static final Duration TTL = Duration.ofMinutes(30);

    private final PopularVideoService popularVideoService;
    private final StringRedisTemplate redisTemplate;
    private final VideoSnapshotRepository videoSnapshotRepository;
    private final ObjectMapper objectMapper;

    @Value("${tubeten.scheduler.regions}")
    private List<String> regions;

    @Value("${tubeten.scheduler.categories:}")
    private List<String> categories;

    @Scheduled(cron = "0 0/30 * * * *")
    public void fetchAndCacheTop100() {
        log.info("🔄 유튜브 Top100 수집 시작");

        for (String regionRaw : regions) {
            String region = normRegion(regionRaw);

            // 카테고리 목록 + :all 워밍
            List<String> cats = new ArrayList<>(categories);
            if (cats.isEmpty() || cats.getFirst() == null) {
                cats = new ArrayList<>();
            }
            if (!cats.contains(null)) cats.add(null);

            for (String categoryIdRaw : cats) {
                String categoryId = normCat(categoryIdRaw);

                try {
                    List<PopularVideoResponse> top100 =
                            popularVideoService.getPopularVideosRaw(region, categoryId);

                    String redisKey = key(region, categoryId);
                    String json = objectMapper.writeValueAsString(top100);
                    redisTemplate.opsForValue().set(redisKey, json, TTL);
                    log.info("✅ Redis 저장 완료: {}", redisKey);

                    // 스냅샷 일괄 저장
                    LocalDateTime now = LocalDateTime.now();
                    List<VideoSnapshot> batch = new ArrayList<>(top100.size());
                    int rank = 1;
                    for (PopularVideoResponse v : top100) {
                        batch.add(VideoSnapshot.builder()
                                .videoId(v.videoId())
                                .title(v.title())
                                .channelTitle(v.channelTitle())
                                .rank(rank++)
                                .viewCount(v.viewCount())
                                .regionCode(region)
                                .categoryId(categoryId)
                                .snapshotTime(now)
                                .build());
                    }
                    if (!batch.isEmpty()) {
                        videoSnapshotRepository.saveAll(batch);
                        log.info("✅ DB Snapshot 저장 완료 - region={}, category={}", region, categoryId);
                    } else {
                        log.info("ℹ️ 수집 결과 없음 - region={}, category={}", region, categoryId);
                    }

                } catch (JsonProcessingException e) {
                    log.error("❌ JSON 변환 실패 - region={}, category={}", region, categoryId, e);
                } catch (Exception e) {
                    log.error("❌ 스케줄러 실행 실패 - region={}, category={}", region, categoryId, e);
                }
            }
        }
        log.info("🎉 유튜브 Top100 수집 완료");
    }

    private static String normRegion(String region) {
        return region == null ? "KR" : region.toUpperCase();
    }
    private static String normCat(String cat) {
        return (cat == null || cat.isBlank()) ? null : cat.trim();
    }
    private static String key(String region, String categoryId) {
        return "top100:" + region + (categoryId != null ? ":" + categoryId : ":all");
    }
}