package com.tubeten.ten.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.tubeten.ten.api.dto.PopularVideoResponse;
import com.tubeten.ten.api.dto.PopularVideoWithTrend;
import com.tubeten.ten.api.repository.VideoSnapshotRepository;
import com.tubeten.ten.config.YoutubeApiConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
public class PopularVideoService {

    private final YoutubeApiConfig youtubeApiConfig;
    private final PopularVideoTrendService popularVideoTrendService;
    private final VideoSnapshotRepository videoSnapshotRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    // 공통 데이터 요청 로직
    private List<PopularVideoResponse> fetchYoutubeTop10(String regionCode, String categoryId) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(youtubeApiConfig.getBaseUrl() + "/videos")
                .queryParam("part", "snippet,statistics")
                .queryParam("chart", "mostPopular")
                .queryParam("regionCode", regionCode)
                .queryParam("maxResults", 10)
                .queryParam("key", youtubeApiConfig.getKey());

        if (categoryId != null && !categoryId.isBlank()) {
            builder.queryParam("videoCategoryId", categoryId);
        }

        String url = builder.toUriString();
        ResponseEntity<JsonNode> response = restTemplate.getForEntity(url, JsonNode.class);
        JsonNode items = response.getBody().get("items");

        return StreamSupport.stream(items.spliterator(), false)
                .map(PopularVideoResponse::from)
                .toList();
    }

    // 스케줄러 등에서 순수 데이터만 필요할 때 사용
    public List<PopularVideoResponse> getPopularVideosRaw(String regionCode, String categoryId) {
        return fetchYoutubeTop10(regionCode, categoryId);
    }

    // 사용자 요청 시 트렌드 비교 자동 적용
    public List<PopularVideoWithTrend> getPopularVideosWithAutoTrend(String regionCode, String categoryId) {
        boolean existsSnapshot = videoSnapshotRepository.existsByRegionCodeAndCategoryId(regionCode, categoryId);

        if (existsSnapshot) {
            return popularVideoTrendService.getTop10WithTrend(regionCode, categoryId);
        } else {
            return fetchYoutubeTop10(regionCode, categoryId).stream()
                    .map(video -> PopularVideoWithTrend.of(video, "new"))
                    .toList();
        }
    }
}
