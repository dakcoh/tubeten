package com.tubeten.ten.batch;

import com.tubeten.ten.api.popularvideo.dto.PopularVideoResponse;
import com.tubeten.ten.api.popularvideo.service.PopularTopQueryService;
import com.tubeten.ten.api.popularvideo.repository.VideoSnapshotRepository;
import com.tubeten.ten.domain.VideoSnapshot;
import com.tubeten.ten.exception.TubetenException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PopularVideoScheduler {

    private final PopularTopQueryService topQuery;
    private final VideoSnapshotRepository videoSnapshotRepository;

    @Value("${tubeten.scheduler.regions:KR,US,JP}")
    private List<String> regions;

    @Value("${tubeten.scheduler.categories:10,20,24}")
    private List<String> categories;

    @Scheduled(cron = "0 0/30 * * * *")
    @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
        name = "tubeten.scheduler.enabled", 
        havingValue = "true", 
        matchIfMissing = true
    )
    public void warmupAndSnapshotTop100() {
        LocalDateTime startTime = LocalDateTime.now();
        log.info("🔄 유튜브 Top200 웜업 시작 - {}", startTime);
        
        int successCount = 0;
        int failCount = 0;
        
        try {
            if (regions == null || regions.isEmpty()) {
                log.warn("설정된 지역이 없습니다. 기본값 KR 사용");
                regions = List.of("KR");
            }

            for (String regionRaw : regions) {
                String region = normRegion(regionRaw);

                List<String> cats = new ArrayList<>(categories == null ? List.of() : categories);
                if (!cats.contains(null)) cats.add(null); // all 포함

                for (String categoryIdRaw : cats) {
                    String categoryId = normCat(categoryIdRaw);
                    
                    try {
                        log.debug("웜업 시작 - region: {}, category: {}", region, categoryId);
                        
                        // @Cacheable("top") 경유 → 캐시 웜업 + 데이터 획득 (더 많은 데이터 수집)
                        List<PopularVideoResponse> top200 = topQuery.getCurrentTop(region, categoryId, 200);

                        if (top200.isEmpty()) {
                            log.warn("조회된 데이터가 없습니다 - region: {}, category: {}", region, categoryId);
                            failCount++;
                            continue;
                        }

                        // 스냅샷 저장
                        saveSnapshot(top200, region, categoryId);
                        successCount++;
                        
                        log.debug("웜업 성공 - region: {}, category: {}, count: {}", region, categoryId, top200.size());

                    } catch (TubetenException e) {
                        log.error("❌ 웜업/스냅샷 실패 (TubetenException) - region: {}, category: {}, error: {}", 
                                region, categoryId, e.getMessage());
                        failCount++;
                        
                        // YouTube API 할당량 초과 시 전체 중단
                        if (e.getErrorCode().getCode().equals("YOUTUBE_002")) {
                            log.error("YouTube API 할당량 초과로 스케줄러 중단");
                            break;
                        }
                        
                    } catch (Exception e) {
                        log.error("❌ 웜업/스냅샷 실패 (예상치 못한 오류) - region: {}, category: {}", 
                                region, categoryId, e);
                        failCount++;
                    }
                    
                    // 각 요청 간 짧은 대기 (API 부하 방지)
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.warn("스케줄러 인터럽트됨");
                        return;
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("스케줄러 실행 중 예상치 못한 오류", e);
        } finally {
            LocalDateTime endTime = LocalDateTime.now();
            long durationSeconds = java.time.Duration.between(startTime, endTime).getSeconds();
            
            log.info("🎉 유튜브 Top200 웜업 완료 - 성공: {}, 실패: {}, 소요시간: {}초", 
                    successCount, failCount, durationSeconds);
        }
    }

    @Transactional
    protected void saveSnapshot(List<PopularVideoResponse> list, String region, String categoryId) {
        if (list == null || list.isEmpty()) {
            log.debug("스냅샷 생략: 빈 목록 - region: {}, category: {}", region, categoryId);
            return;
        }
        
        try {
            LocalDateTime now = LocalDateTime.now();
            List<VideoSnapshot> batch = new ArrayList<>(list.size());
            
            for (int i = 0; i < list.size(); i++) {
                var v = list.get(i);
                
                // 필수 데이터 검증
                if (v.videoId() == null || v.videoId().isBlank()) {
                    log.warn("비디오 ID가 없는 항목 건너뛰기 - index: {}, region: {}, category: {}", 
                            i, region, categoryId);
                    continue;
                }
                
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
            
            if (batch.isEmpty()) {
                log.warn("저장할 유효한 스냅샷 데이터가 없습니다 - region: {}, category: {}", region, categoryId);
                return;
            }
            
            videoSnapshotRepository.saveAll(batch);
            log.info("✅ DB Snapshot 저장 완료 - region: {}, category: {}, count: {}", 
                    region, categoryId, batch.size());
                    
        } catch (DataAccessException e) {
            log.error("스냅샷 저장 중 데이터베이스 오류 - region: {}, category: {}", region, categoryId, e);
            throw new TubetenException(com.tubeten.ten.exception.ErrorCode.DATABASE_ERROR, 
                    "스냅샷 저장 실패", e);
        } catch (Exception e) {
            log.error("스냅샷 저장 중 예상치 못한 오류 - region: {}, category: {}", region, categoryId, e);
            throw new TubetenException(com.tubeten.ten.exception.ErrorCode.INTERNAL_SERVER_ERROR, 
                    "스냅샷 저장 실패", e);
        }
    }

    private static String normRegion(String region) {
        return region == null ? "KR" : region.toUpperCase();
    }
    private static String normCat(String cat) {
        return (cat == null || cat.isBlank()) ? null : cat.trim();
    }
}