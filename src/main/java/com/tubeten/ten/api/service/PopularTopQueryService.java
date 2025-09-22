package com.tubeten.ten.api.service;

import com.tubeten.ten.api.dto.PopularVideoResponse;
import com.tubeten.ten.config.YoutubeApiConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import java.net.URI;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PopularTopQueryService {
    private final YoutubeApiConfig youtubeApiConfig;
    private final RestTemplate restTemplate = new RestTemplate();

    @Cacheable(
            cacheNames = "top",
            key = "T(java.lang.String).format('%s:%s:%s', #region?:'KR', (#categoryId?:'all'), #size)",
            sync = true
    )
    public List<PopularVideoResponse> getCurrentTop(String region, String categoryId, int size) {
        String r = region == null ? "KR" : region.toUpperCase();
        String c = (categoryId == null || categoryId.isBlank()) ? null : categoryId.trim();
        int n = Math.max(1, Math.min(size, 100));
        List<PopularVideoResponse> list = fetchYoutubeTop100(r, c);
        return list.size() > n ? list.subList(0, n) : list;
    }

    private List<PopularVideoResponse> fetchYoutubeTop100(String r, String c) {
        List<PopularVideoResponse> out = new ArrayList<>(100);
        String pageToken = null;
        for (int page = 0; page < 2; page++) {
            var b = UriComponentsBuilder
                    .fromUri(URI.create(youtubeApiConfig.getBaseUrl() + "/videos"))
                    .queryParam("part", "snippet,statistics,contentDetails")
                    .queryParam("chart", "mostPopular")
                    .queryParam("regionCode", r)
                    .queryParam("maxResults", 50)
                    .queryParam("key", youtubeApiConfig.getKey());
            if (c != null) b.queryParam("videoCategoryId", c);
            if (pageToken != null) b.queryParam("pageToken", pageToken);

            ResponseEntity<com.fasterxml.jackson.databind.JsonNode> res =
                    restTemplate.getForEntity(b.toUriString(), com.fasterxml.jackson.databind.JsonNode.class);
            var body = res.getBody();
            if (body == null || body.get("items") == null) break;

            for (var item : body.get("items")) out.add(PopularVideoResponse.from(item));
            pageToken = body.has("nextPageToken") ? body.get("nextPageToken").asText(null) : null;
            if (pageToken == null) break;
        }
        return out.size() > 100 ? out.subList(0, 100) : out;
    }
}
