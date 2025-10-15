package com.tubeten.ten.api.popularvideo.service;

import com.tubeten.ten.api.popularvideo.dto.PopularVideoResponse;
import com.tubeten.ten.api.popularvideo.repository.VideoSnapshotRepository;
import com.tubeten.ten.domain.PopularVideoWithTrend;
import com.tubeten.ten.domain.VideoSnapshot;
import com.tubeten.ten.exception.ErrorCode;
import com.tubeten.ten.exception.TubetenException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PopularVideoTrendService {

    private final PopularTopQueryService topQuery;
    private final VideoSnapshotRepository snapshotRepository;

    public List<PopularVideoWithTrend> getTopWithTrend(String region, String categoryId, int size) {
        try {
            String r = region == null ? "KR" : region.toUpperCase();
            String c = (categoryId == null || categoryId.isBlank()) ? null : categoryId.trim();
            
            log.debug("트렌드 계산 시작 - region: {}, category: {}, size: {}", r, c, size);
            
            // 현재 인기 영상 목록 조회 (확장된 수집 시도)
            List<PopularVideoResponse> currentList;
            try {
                currentList = topQuery.getCurrentTop(r, c, size);
                
                // 데이터가 부족하면 확장된 수집 시도
                if (currentList.size() < Math.min(50, size)) {
                    log.info("트렌드 계산용 데이터 부족으로 확장 수집 시도 - 현재: {}개", currentList.size());
                    currentList = topQuery.getExtendedTop(r, c, size);
                }
            } catch (Exception e) {
                log.warn("기본 조회 실패, 확장 수집으로 fallback", e);
                currentList = topQuery.getExtendedTop(r, c, size);
            }
            if (currentList.isEmpty()) {
                log.debug("현재 인기 영상이 없어 빈 목록 반환");
                return List.of();
            }

            // 비교 기준 스냅샷 조회 (30분 전)
            LocalDateTime compareBase = LocalDateTime.now().minusMinutes(30);
            
            try {
                var latestSnapshotOpt = snapshotRepository
                        .findTop1ByRegionCodeAndCategoryIdAndSnapshotTimeLessThanOrderBySnapshotTimeDesc(r, c, compareBase);

                if (latestSnapshotOpt.isEmpty()) {
                    log.debug("비교할 이전 스냅샷이 없어 모든 영상을 신규로 표시");
                    return markAllAsNew(currentList);
                }

                LocalDateTime compareTime = latestSnapshotOpt.get().getSnapshotTime();
                log.debug("비교 기준 시간: {}", compareTime);
                
                List<VideoSnapshot> previousSnapshots =
                        snapshotRepository.findBySnapshotTimeAndRegionCodeAndCategoryId(compareTime, r, c);

                if (previousSnapshots.isEmpty()) {
                    log.debug("비교 기준 시간의 스냅샷 데이터가 없어 모든 영상을 신규로 표시");
                    return markAllAsNew(currentList);
                }

                // 이전 순위 맵 생성
                Map<String, Integer> previousRankMap = previousSnapshots.stream()
                        .filter(snapshot -> snapshot.getVideoId() != null) // null 체크
                        .collect(Collectors.toMap(VideoSnapshot::getVideoId, VideoSnapshot::getRank));

                // 트렌드 계산
                List<PopularVideoWithTrend> result = new ArrayList<>(currentList.size());
                for (int i = 0; i < currentList.size(); i++) {
                    PopularVideoResponse video = currentList.get(i);
                    if (video.videoId() == null) {
                        log.warn("비디오 ID가 null인 항목 건너뛰기 - index: {}", i);
                        continue;
                    }
                    
                    int currentRank = i + 1;
                    Integer prev = previousRankMap.get(video.videoId());

                    String trend; 
                    Integer diff = null;
                    
                    if (prev == null) { 
                        trend = "new"; 
                    } else if (currentRank < prev) { 
                        trend = "↑"; 
                        diff = prev - currentRank; 
                    } else if (currentRank > prev) { 
                        trend = "↓"; 
                        diff = currentRank - prev; 
                    } else { 
                        trend = "→"; 
                        diff = 0; 
                    }

                    result.add(PopularVideoWithTrend.of(video, trend, diff));
                }
                
                log.debug("트렌드 계산 완료 - 결과 수: {}", result.size());
                return result;
                
            } catch (DataAccessException e) {
                log.error("스냅샷 조회 중 데이터베이스 오류 - region: {}, category: {}", r, c, e);
                // DB 오류 시 현재 데이터만으로라도 응답 (모든 영상을 신규로 표시)
                log.warn("DB 오류로 인해 트렌드 없이 현재 데이터만 반환");
                return markAllAsNew(currentList);
            }
            
        } catch (TubetenException e) {
            throw e; // 이미 처리된 예외는 그대로 전파
        } catch (Exception e) {
            log.error("트렌드 계산 중 예상치 못한 오류 - region: {}, category: {}", region, categoryId, e);
            throw new TubetenException(ErrorCode.INTERNAL_SERVER_ERROR, "트렌드 계산 실패", e);
        }
    }

    private List<PopularVideoWithTrend> markAllAsNew(List<PopularVideoResponse> list) {
        return list.stream().map(v -> PopularVideoWithTrend.of(v, "new", null)).toList();
    }
}
