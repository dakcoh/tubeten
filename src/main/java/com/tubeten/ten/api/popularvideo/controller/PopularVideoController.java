package com.tubeten.ten.api.popularvideo.controller;

import com.tubeten.ten.domain.PopularVideoWithTrend;
import com.tubeten.ten.api.popularvideo.service.PopularVideoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class PopularVideoController {

    private final PopularVideoService popularVideoService;

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }

    @GetMapping("/popular")
    public ResponseEntity<List<PopularVideoWithTrend>> getPopularVideos(
            @RequestParam(defaultValue = "KR") String region,
            @RequestParam(required = false) String categoryId,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "50") int limit
    ) {
        log.info("인기 영상 API 호출 - region: {}, category: {}, offset: {}, limit: {}", 
                region, categoryId, offset, limit);
        
        // 성능 최적화: 작은 요청은 빠르게 처리
        int safeLimit = Math.min(limit, 50); // 기본 요청은 50개로 제한하여 응답 속도 향상
                
        List<PopularVideoWithTrend> result = popularVideoService.getPopularVideosWithAutoTrend(
                region, categoryId, offset, safeLimit);
        
        log.info("인기 영상 API 응답 완료 - 요청: {}개, 실제 반환: {}개", limit, result.size());
        return ResponseEntity.ok(result);
    }
    
    /**
     * 최대한 많은 데이터를 가져오는 엔드포인트
     */
    @GetMapping("/popular/extended")
    public ResponseEntity<List<PopularVideoWithTrend>> getExtendedPopularVideos(
            @RequestParam(defaultValue = "KR") String region,
            @RequestParam(required = false) String categoryId,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "100") int limit
    ) {
        log.info("확장 인기 영상 API 호출 - region: {}, category: {}, offset: {}, limit: {}", 
                region, categoryId, offset, limit);
                
        List<PopularVideoWithTrend> result = popularVideoService.getPopularVideosWithAutoTrend(
                region, categoryId, offset, 200); // 항상 200개 수집 시도
                
        // 요청된 limit만큼만 반환
        if (result.size() > limit) {
            result = new ArrayList<>(result.subList(0, limit));
        }
                
        log.info("확장 인기 영상 API 응답 완료 - 요청: {}개, 실제 반환: {}개", limit, result.size());
        return ResponseEntity.ok(result);
    }
    
    /**
     * 빠른 응답을 위한 경량 엔드포인트 (12개 이하 요청용)
     */
    @GetMapping("/popular/fast")
    public ResponseEntity<List<PopularVideoWithTrend>> getFastPopularVideos(
            @RequestParam(defaultValue = "KR") String region,
            @RequestParam(required = false) String categoryId,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "12") int limit
    ) {
        log.info("빠른 인기 영상 API 호출 - region: {}, category: {}, offset: {}, limit: {}", 
                region, categoryId, offset, limit);
        
        // 작은 요청은 최소한의 데이터만 수집하여 빠른 응답
        int fastLimit = Math.min(limit, 20);
        List<PopularVideoWithTrend> result = popularVideoService.getPopularVideosWithAutoTrend(
                region, categoryId, offset, fastLimit);
                
        log.info("빠른 인기 영상 API 응답 완료 - 요청: {}개, 실제 반환: {}개", limit, result.size());
        return ResponseEntity.ok(result);
    }
    
    /**
     * 많은 데이터를 보장하는 새로운 엔드포인트
     */
    @GetMapping("/popular/max")
    public ResponseEntity<List<PopularVideoWithTrend>> getMaxPopularVideos(
            @RequestParam(defaultValue = "KR") String region,
            @RequestParam(required = false) String categoryId,
            @RequestParam(defaultValue = "100") int limit
    ) {
        log.info("최대 인기 영상 API 호출 - region: {}, category: {}, limit: {}", 
                region, categoryId, limit);
                
        // 항상 최대한 많은 데이터 수집
        List<PopularVideoWithTrend> result = popularVideoService.getPopularVideosWithAutoTrend(
                region, categoryId, 0, 200);
                
        // 요청된 limit만큼만 반환
        if (result.size() > limit) {
            result = new ArrayList<>(result.subList(0, limit));
        }
                
        log.info("최대 인기 영상 API 응답 완료 - 요청: {}개, 실제 반환: {}개", limit, result.size());
        return ResponseEntity.ok(result);
    }
}
