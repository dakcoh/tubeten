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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PopularVideoService {

    private final PopularTopQueryService topQuery;
    private final PopularVideoTrendService popularVideoTrendService;
    private final VideoSnapshotRepository videoSnapshotRepository;

    /**
     * 스냅샷 저장 (비동기 트랜잭션 처리)
     */
    @Async("taskExecutor")
    @Transactional
    public void saveSnapshotAsync(List<PopularVideoResponse> videos, String region, String categoryId) {
        if (videos == null || videos.isEmpty()) {
            log.debug("스냅샷 저장 생략 - 비어있는 비디오 목록");
            return;
        }
        
        try {
            String r = region == null ? "KR" : region.toUpperCase();
            String c = (categoryId == null || categoryId.isBlank()) ? null : categoryId.trim();
            LocalDateTime now = LocalDateTime.now();

            List<VideoSnapshot> rows = new ArrayList<>(videos.size());
            for (int i = 0; i < videos.size(); i++) {
                var v = videos.get(i);
                if (v.videoId() == null || v.videoId().isBlank()) {
                    log.warn("비디오 ID가 없는 항목 건너뛰기 - index: {}", i);
                    continue;
                }
                
                rows.add(VideoSnapshot.builder()
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
            
            if (!rows.isEmpty()) {
                videoSnapshotRepository.saveAll(rows);
                log.info("스냅샷 저장 완료 - region: {}, category: {}, count: {}", r, c, rows.size());
            }
            
        } catch (DataAccessException e) {
            log.error("스냅샷 저장 실패 - region: {}, category: {}", region, categoryId, e);
            throw new TubetenException(ErrorCode.DATABASE_ERROR, "스냅샷 저장 중 데이터베이스 오류", e);
        } catch (Exception e) {
            log.error("스냅샷 저장 중 예상치 못한 오류 - region: {}, category: {}", region, categoryId, e);
            throw new TubetenException(ErrorCode.INTERNAL_SERVER_ERROR, "스냅샷 저장 실패", e);
        }
    }

    public List<PopularVideoWithTrend> getPopularVideosWithAutoTrend(String regionCode, String categoryId, int offset, int limit) {
        try {
            // 파라미터 검증
            validatePaginationParameters(offset, limit);
            
            String r = regionCode == null ? "KR" : regionCode.toUpperCase();
            String cNorm = (categoryId == null || categoryId.isBlank()) ? "all" : categoryId.trim();
            
            log.debug("인기 영상 조회 시작 - region: {}, category: {}, offset: {}, limit: {}", 
                    r, cNorm, offset, limit);

            // 100개 이상 확보를 위한 적극적 데이터 수집 전략
            List<PopularVideoResponse> fullTop;
            int targetSize = Math.max(limit + offset, 100); // 최소 100개는 확보
            
            try {
                // 확장 수집을 우선 시도 (더 많은 데이터 확보)
                fullTop = topQuery.getExtendedTop(r, "all".equals(cNorm) ? null : cNorm, 200);
                log.debug("확장 수집: {}개", fullTop.size());
                
                // 확장 수집으로도 부족하면 기본 수집 추가 시도
                if (fullTop.size() < targetSize) {
                    try {
                        List<PopularVideoResponse> basicTop = topQuery.getCurrentTop(r, "all".equals(cNorm) ? null : cNorm, 200);
                        log.debug("기본 수집 추가: {}개", basicTop.size());
                        
                        // 중복 제거하면서 병합
                        Set<String> existingIds = fullTop.stream()
                                .map(PopularVideoResponse::videoId)
                                .collect(java.util.stream.Collectors.toSet());
                        
                        for (PopularVideoResponse video : basicTop) {
                            if (!existingIds.contains(video.videoId()) && fullTop.size() < 200) {
                                fullTop.add(video);
                                existingIds.add(video.videoId());
                            }
                        }
                        log.debug("병합 완료: {}개", fullTop.size());
                    } catch (Exception ex) {
                        log.warn("기본 수집 추가 실패", ex);
                    }
                }
            } catch (Exception e) {
                log.warn("확장 수집 실패, 기본 수집으로 fallback", e);
                try {
                    fullTop = topQuery.getCurrentTop(r, "all".equals(cNorm) ? null : cNorm, 200);
                    log.debug("기본 수집 fallback: {}개", fullTop.size());
                } catch (Exception ex) {
                    log.error("모든 수집 방법 실패", ex);
                    throw new TubetenException(ErrorCode.YOUTUBE_API_ERROR, "데이터 수집 실패", ex);
                }
            }
            
            if (fullTop.isEmpty()) {
                log.warn("조회된 인기 영상이 없습니다 - region: {}, category: {}", r, cNorm);
                return List.of();
            }

            // 스냅샷 저장 임시 비활성화 (성능 테스트용)
            // TODO: 비동기 설정 완료 후 다시 활성화
            /*
            try {
                saveSnapshotAsync(fullTop, r, "all".equals(cNorm) ? null : cNorm);
            } catch (Exception e) {
                log.error("비동기 스냅샷 저장 시작 실패 - region: {}, category: {}", r, cNorm, e);
            }
            */
            log.debug("스냅샷 저장 생략 (성능 테스트 모드)");

            // 트렌드 계산 - 필요한 만큼만 계산
            int trendCalcSize = Math.min(fullTop.size(), Math.max(offset + limit, 100));
            List<PopularVideoWithTrend> withTrend = popularVideoTrendService.getTopWithTrend(r, "all".equals(cNorm) ? null : cNorm, trendCalcSize);

            // 페이징 처리
            int toIndex = Math.min(offset + limit, withTrend.size());
            if (offset >= withTrend.size()) {
                log.debug("요청한 offset이 결과 크기를 초과 - offset: {}, size: {}", offset, withTrend.size());
                return List.of();
            }
            
            List<PopularVideoWithTrend> result = new ArrayList<>(withTrend.subList(offset, toIndex));
            log.debug("인기 영상 조회 완료 - 결과 수: {}", result.size());
            
            return result;
            
        } catch (TubetenException e) {
            log.error("인기 영상 조회 실패 - region: {}, category: {}", regionCode, categoryId, e);
            throw e;
        } catch (Exception e) {
            log.error("인기 영상 조회 중 예상치 못한 오류 - region: {}, category: {}", regionCode, categoryId, e);
            throw new TubetenException(ErrorCode.INTERNAL_SERVER_ERROR, "인기 영상 조회 실패", e);
        }
    }
    
    /**
     * 페이징 파라미터 검증
     */
    private void validatePaginationParameters(int offset, int limit) {
        if (offset < 0) {
            throw new TubetenException(ErrorCode.INVALID_PARAMETER, "offset은 0 이상이어야 합니다: " + offset);
        }
        if (limit <= 0 || limit > 200) {
            throw new TubetenException(ErrorCode.INVALID_PARAMETER, "limit은 1-200 사이의 값이어야 합니다: " + limit);
        }
    }
}