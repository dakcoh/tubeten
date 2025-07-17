package com.tubeten.ten.api.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tubeten.ten.api.dto.PopularVideoResponse;
import com.tubeten.ten.api.dto.PopularVideoWithTrend;
import com.tubeten.ten.domain.VideoSnapshot;
import com.tubeten.ten.api.repository.VideoSnapshotRepository;
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

    public List<PopularVideoWithTrend> getTop10WithTrend(String region, String categoryId) {
        // 1. Redis에서 현재 Top 10 가져오기 (category 포함된 key)
        String redisKey = buildRedisKey(region, categoryId);
        String json = redisTemplate.opsForValue().get(redisKey);

        if (json == null) {
            throw new IllegalStateException("Redis에 Top10 캐시가 없습니다: " + redisKey);
        }

        List<PopularVideoResponse> currentList;
        try {
            currentList = objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            throw new RuntimeException("Redis JSON 파싱 실패", e);
        }

        // 2. 1시간 전 snapshot 조회 (region + categoryId 기준)
        LocalDateTime thirtyMinutesAgo = LocalDateTime.now().minusMinutes(30);

        Optional<VideoSnapshot> latestSnapshotOpt =
                snapshotRepository.findTop1ByRegionCodeAndCategoryIdAndSnapshotTimeLessThanOrderBySnapshotTimeDesc(
                        region, categoryId, thirtyMinutesAgo);

        if (latestSnapshotOpt.isEmpty()) {
            return markAllAsNew(currentList); // 혹은 전부 '→' 처리
        }

        LocalDateTime compareTime = latestSnapshotOpt.get().getSnapshotTime();
        List<VideoSnapshot> previousSnapshots =
                snapshotRepository.findBySnapshotTimeAndRegionCodeAndCategoryId(compareTime, region, categoryId);

        // 3. 이전 순위 맵핑
        Map<String, Integer> previousRankMap = previousSnapshots.stream()
                .collect(Collectors.toMap(VideoSnapshot::getVideoId, VideoSnapshot::getRank));

        // 4. 트렌드 계산
        List<PopularVideoWithTrend> result = new ArrayList<>();
        for (int i = 0; i < currentList.size(); i++) {
            PopularVideoResponse video = currentList.get(i);
            int currentRank = i + 1;
            Integer previousRank = previousRankMap.get(video.getVideoId());

            String trend;
            if (previousRank == null) trend = "new";
            else if (currentRank < previousRank) trend = "↑";
            else if (currentRank > previousRank) trend = "↓";
            else trend = "→";

            result.add(PopularVideoWithTrend.of(video, trend));
        }

        return result;
    }

    private String buildRedisKey(String region, String categoryId) {
        return "top10:" + region + (categoryId != null ? ":" + categoryId : ":all");
    }

    private List<PopularVideoWithTrend> markAllAsNew(List<PopularVideoResponse> list) {
        return list.stream()
                .map(video -> PopularVideoWithTrend.of(video, "new"))
                .collect(Collectors.toList());
    }

    public List<PopularVideoWithTrend> addTrend(List<PopularVideoResponse> current, String regionCode, String categoryId) {
        // 최신 snapshot 조회
        List<VideoSnapshot> lastSnapshots = snapshotRepository
                .findTop10ByRegionCodeAndCategoryIdOrderBySnapshotTimeDesc(regionCode, categoryId);

        // videoId → rank 매핑
        Map<String, Integer> previousRankMap = new HashMap<>();
        for (VideoSnapshot snapshot : lastSnapshots) {
            previousRankMap.put(snapshot.getVideoId(), snapshot.getRank());
        }

        List<PopularVideoWithTrend> result = new ArrayList<>();
        for (int i = 0; i < current.size(); i++) {
            PopularVideoResponse video = current.get(i);
            int currentRank = i + 1;

            String trend;
            Integer previousRank = previousRankMap.get(video.getVideoId());

            if (previousRank == null) {
                trend = "new";
            } else if (currentRank < previousRank) {
                trend = "↑";
            } else if (currentRank > previousRank) {
                trend = "↓";
            } else {
                trend = "→";
            }

            result.add(PopularVideoWithTrend.of(video, trend));
        }

        return result;
    }
}
