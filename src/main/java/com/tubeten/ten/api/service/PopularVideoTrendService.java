package com.tubeten.ten.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tubeten.ten.api.dto.PopularVideoResponse;
import com.tubeten.ten.domain.PopularVideoWithTrend;
import com.tubeten.ten.api.repository.VideoSnapshotRepository;
import com.tubeten.ten.domain.VideoSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PopularVideoTrendService {

    private final PopularTopQueryService topQuery;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final VideoSnapshotRepository snapshotRepository;

    public List<PopularVideoWithTrend> getTopWithTrend(String region, String categoryId, int size) {
        String r = region == null ? "KR" : region.toUpperCase();
        String c = (categoryId == null || categoryId.isBlank()) ? null : categoryId.trim();
        List<PopularVideoResponse> redisKey = topQuery.getCurrentTop(r, c, size);

        String json = redisTemplate.opsForValue().get(redisKey);
        if (json == null) throw new IllegalStateException("Redis에 Top" + size + " 캐시가 없습니다: " + redisKey);

        List<PopularVideoResponse> currentList;
        try {
            currentList = objectMapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<>() {});
        } catch (Exception e) {
            throw new RuntimeException("Redis JSON 파싱 실패", e);
        }

        LocalDateTime compareBase = LocalDateTime.now().minusMinutes(30);
        var latestSnapshotOpt = snapshotRepository
                .findTop1ByRegionCodeAndCategoryIdAndSnapshotTimeLessThanOrderBySnapshotTimeDesc(r, c, compareBase);

        if (latestSnapshotOpt.isEmpty()) return markAllAsNew(currentList);

        LocalDateTime compareTime = latestSnapshotOpt.get().getSnapshotTime();
        List<VideoSnapshot> previousSnapshots =
                snapshotRepository.findBySnapshotTimeAndRegionCodeAndCategoryId(compareTime, r, c);

        Map<String, Integer> previousRankMap = previousSnapshots.stream()
                .collect(Collectors.toMap(VideoSnapshot::getVideoId, VideoSnapshot::getRank));

        List<PopularVideoWithTrend> result = new ArrayList<>();
        for (int i = 0; i < currentList.size(); i++) {
            PopularVideoResponse video = currentList.get(i);
            int currentRank = i + 1;
            Integer previousRank = previousRankMap.get(video.videoId());

            String trend;
            Integer rankDiff = null;
            if (previousRank == null) {
                trend = "new";
            } else {
                rankDiff = Math.abs(currentRank - previousRank);
                if (currentRank < previousRank) trend = "↑";
                else if (currentRank > previousRank) trend = "↓";
                else { trend = "→"; rankDiff = 0; }
            }
            result.add(PopularVideoWithTrend.of(video, trend, rankDiff));
        }
        return result;
    }

    private List<PopularVideoWithTrend> markAllAsNew(List<PopularVideoResponse> list) {
        return list.stream()
                .map(video -> PopularVideoWithTrend.of(video, "new", null))
                .collect(Collectors.toList());
    }
}
