package com.tubeten.ten.batch;

import com.tubeten.ten.api.popularvideo.dto.PopularVideoResponse;
import com.tubeten.ten.api.popularvideo.service.PopularTopQueryService;
import com.tubeten.ten.api.popularvideo.repository.VideoSnapshotRepository;
import com.tubeten.ten.domain.VideoSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PopularVideoScheduler {

    private final PopularTopQueryService topQuery;
    private final VideoSnapshotRepository videoSnapshotRepository;

    @Value("${tubeten.scheduler.regions}")
    private List<String> regions;

    @Value("${tubeten.scheduler.categories:}")
    private List<String> categories;

    @Scheduled(cron = "0 0/30 * * * *")
    public void warmupAndSnapshotTop100() {
        log.info("🔄 유튜브 Top100 웜업 시작");

        for (String regionRaw : regions) {
            String region = normRegion(regionRaw);

            List<String> cats = new ArrayList<>(categories == null ? List.of() : categories);
            if (!cats.contains(null)) cats.add(null); // all 포함

            for (String categoryIdRaw : cats) {
                String categoryId = normCat(categoryIdRaw);
                try {
                    // ✅ @Cacheable("top") 경유 → 캐시 웜업 + 데이터 획득
                    List<PopularVideoResponse> top100 = topQuery.getCurrentTop(region, categoryId, 100);

                    // 선택: 스냅샷 저장
                    saveSnapshot(top100, region, categoryId);

                } catch (Exception e) {
                    log.error("❌ 웜업/스냅샷 실패 - region={}, category={}", region, categoryId, e);
                }
            }
        }
        log.info("🎉 유튜브 Top100 웜업 완료");
    }

    private void saveSnapshot(List<PopularVideoResponse> list, String region, String categoryId) {
        if (list == null || list.isEmpty()) {
            log.info("ℹ️ 스냅샷 생략: 빈 목록 - region={}, category={}", region, categoryId);
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        List<VideoSnapshot> batch = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            var v = list.get(i);
            batch.add(VideoSnapshot.builder()
                    .videoId(v.videoId())
                    .title(v.title())
                    .channelTitle(v.channelTitle())
                    .rank(i + 1)
                    .viewCount(v.viewCount())
                    .regionCode(region)
                    .categoryId(categoryId)
                    .snapshotTime(now)
                    .build());
        }
        videoSnapshotRepository.saveAll(batch);
        log.info("✅ DB Snapshot 저장 완료 - region={}, category={}, count={}", region, categoryId, batch.size());
    }

    private static String normRegion(String region) {
        return region == null ? "KR" : region.toUpperCase();
    }
    private static String normCat(String cat) {
        return (cat == null || cat.isBlank()) ? null : cat.trim();
    }
}