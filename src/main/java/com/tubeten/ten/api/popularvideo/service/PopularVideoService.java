package com.tubeten.ten.api.popularvideo.service;

import com.tubeten.ten.api.popularvideo.dto.PopularVideoResponse;
import com.tubeten.ten.api.popularvideo.repository.VideoSnapshotRepository;
import com.tubeten.ten.domain.PopularVideoWithTrend;
import com.tubeten.ten.domain.VideoSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PopularVideoService {

    private final PopularTopQueryService topQuery;
    private final PopularVideoTrendService popularVideoTrendService;
    private final VideoSnapshotRepository videoSnapshotRepository;

    // 필요하면 캐시 히트/미스와 무관하게 최신 결과를 스냅샷으로 저장
    private void saveSnapshot(List<PopularVideoResponse> videos, String region, String categoryId) {
        String r = region == null ? "KR" : region.toUpperCase();
        String c = (categoryId == null || categoryId.isBlank()) ? null : categoryId.trim();
        LocalDateTime now = LocalDateTime.now();
        if (videos.isEmpty()) return;

        List<VideoSnapshot> rows = new ArrayList<>(videos.size());
        for (int i = 0; i < videos.size(); i++) {
            var v = videos.get(i);
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
        videoSnapshotRepository.saveAll(rows);
    }

    public List<PopularVideoWithTrend> getPopularVideosWithAutoTrend(String regionCode, String categoryId, int offset, int limit) {
        String r = regionCode == null ? "KR" : regionCode.toUpperCase();
        String cNorm = (categoryId == null || categoryId.isBlank()) ? "all" : categoryId.trim();

        // 캐시된 Top 100 조회(@Cacheable, TTL은 CacheConfig에서)
        List<PopularVideoResponse> fullTop = topQuery.getCurrentTop(r, "all".equals(cNorm) ? null : cNorm, 100);
        if (fullTop.isEmpty()) return List.of();

        saveSnapshot(fullTop, r, "all".equals(cNorm) ? null : cNorm);

        // 트렌드 계산
        List<PopularVideoWithTrend> withTrend = popularVideoTrendService.getTopWithTrend(r, "all".equals(cNorm) ? null : cNorm, 100);

        int toIndex = Math.min(offset + limit, withTrend.size());
        if (offset >= toIndex) return List.of();
        return withTrend.subList(offset, toIndex);
    }
}