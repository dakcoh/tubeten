package com.tubeten.ten.api.service;

import com.fasterxml.jackson.core.type.TypeReference;
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

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final VideoSnapshotRepository snapshotRepository;

    public List<PopularVideoWithTrend> getTopWithTrend(String region, String categoryId, int size) {
        String redisKey = "top" + size + ":" + region + (categoryId != null ? ":" + categoryId : ":all");
        String json = redisTemplate.opsForValue().get(redisKey);

        if (json == null) {
            throw new IllegalStateException("Redis에 Top" + size + " 캐시가 없습니다: " + redisKey);
        }

        List<PopularVideoResponse> currentList;
        try {
            currentList = objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            throw new RuntimeException("Redis JSON 파싱 실패", e);
        }

        // 30분 전 snapshot 기준
        LocalDateTime compareBase = LocalDateTime.now().minusMinutes(30);
        Optional<VideoSnapshot> latestSnapshotOpt =
                snapshotRepository.findTop1ByRegionCodeAndCategoryIdAndSnapshotTimeLessThanOrderBySnapshotTimeDesc(
                        region, categoryId, compareBase);

        if (latestSnapshotOpt.isEmpty()) {
            return markAllAsNew(currentList);
        }

        LocalDateTime compareTime = latestSnapshotOpt.get().getSnapshotTime();
        List<VideoSnapshot> previousSnapshots =
                snapshotRepository.findBySnapshotTimeAndRegionCodeAndCategoryId(compareTime, region, categoryId);

        Map<String, Integer> previousRankMap = previousSnapshots.stream()
                .collect(Collectors.toMap(VideoSnapshot::getVideoId, VideoSnapshot::getRank));

        List<PopularVideoWithTrend> result = new ArrayList<>();
        for (int i = 0; i < currentList.size(); i++) {
            PopularVideoResponse video = currentList.get(i);
            int currentRank = i + 1;
            Integer previousRank = previousRankMap.get(video.getVideoId());

            String trend;
            Integer rankDiff = null;

            if (previousRank == null) {
                trend = "new";
            } else {
                rankDiff = Math.abs(currentRank - previousRank);
                if (currentRank < previousRank) {
                    trend = "↑";
                } else if (currentRank > previousRank) {
                    trend = "↓";
                } else {
                    trend = "→";
                    rankDiff = 0;
                }
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
