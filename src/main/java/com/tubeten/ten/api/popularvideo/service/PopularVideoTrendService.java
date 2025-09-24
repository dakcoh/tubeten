package com.tubeten.ten.api.popularvideo.service;

import com.tubeten.ten.api.popularvideo.dto.PopularVideoResponse;
import com.tubeten.ten.api.popularvideo.repository.VideoSnapshotRepository;
import com.tubeten.ten.domain.PopularVideoWithTrend;
import com.tubeten.ten.domain.VideoSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PopularVideoTrendService {

    private final PopularTopQueryService topQuery;
    private final VideoSnapshotRepository snapshotRepository;

    public List<PopularVideoWithTrend> getTopWithTrend(String region, String categoryId, int size) {
        String r = region == null ? "KR" : region.toUpperCase();
        String c = (categoryId == null || categoryId.isBlank()) ? null : categoryId.trim();
        List<PopularVideoResponse> currentList = topQuery.getCurrentTop(r, c, size);

        // 비교 기준 스냅샷 조회
        LocalDateTime compareBase = LocalDateTime.now().minusMinutes(30);
        var latestSnapshotOpt = snapshotRepository
                .findTop1ByRegionCodeAndCategoryIdAndSnapshotTimeLessThanOrderBySnapshotTimeDesc(r, c, compareBase);

        if (latestSnapshotOpt.isEmpty()) return markAllAsNew(currentList);

        LocalDateTime compareTime = latestSnapshotOpt.get().getSnapshotTime();
        List<VideoSnapshot> previousSnapshots =
                snapshotRepository.findBySnapshotTimeAndRegionCodeAndCategoryId(compareTime, r, c);

        Map<String, Integer> previousRankMap = previousSnapshots.stream()
                .collect(Collectors.toMap(VideoSnapshot::getVideoId, VideoSnapshot::getRank));

        List<PopularVideoWithTrend> result = new ArrayList<>(currentList.size());
        for (int i = 0; i < currentList.size(); i++) {
            PopularVideoResponse video = currentList.get(i);
            int currentRank = i + 1;
            Integer prev = previousRankMap.get(video.videoId());

            String trend; Integer diff = null;
            if (prev == null) { trend = "new"; }
            else if (currentRank < prev) { trend = "↑"; diff = prev - currentRank; }
            else if (currentRank > prev) { trend = "↓"; diff = currentRank - prev; }
            else { trend = "→"; diff = 0; }

            result.add(PopularVideoWithTrend.of(video, trend, diff));
        }
        return result;
    }

    private List<PopularVideoWithTrend> markAllAsNew(List<PopularVideoResponse> list) {
        return list.stream().map(v -> PopularVideoWithTrend.of(v, "new", null)).toList();
    }
}
