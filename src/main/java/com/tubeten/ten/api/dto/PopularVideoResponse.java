package com.tubeten.ten.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PopularVideoResponse {

    private String videoId;
    private String title;
    private String channelTitle;
    private long viewCount;
    private long likeCount;
    private long commentCount;
    private String videoUrl;
    private boolean isShorts;

    public static PopularVideoResponse from(JsonNode item) {
        String videoId = item.get("id").asText();
        JsonNode snippet = item.get("snippet");
        JsonNode statistics = item.get("statistics");

        String title = snippet.get("title").asText();
        String channelTitle = snippet.get("channelTitle").asText();

        long viewCount = statistics.has("viewCount") ? statistics.get("viewCount").asLong() : 0;
        long likeCount = statistics.has("likeCount") ? statistics.get("likeCount").asLong() : 0;
        long commentCount = statistics.has("commentCount") ? statistics.get("commentCount").asLong() : 0;

        // Shorts 여부: 썸네일 URL이나 title, tags 등에 기반한 간단 필터
        boolean isShorts = false;
        if (snippet.has("tags")) {
            for (JsonNode tag : snippet.get("tags")) {
                if (tag.asText().toLowerCase().contains("shorts")) {
                    isShorts = true;
                    break;
                }
            }
        }
        // fallback: 제목에 "[Shorts]" 포함 등
        if (title.toLowerCase().contains("shorts")) {
            isShorts = true;
        }

        return PopularVideoResponse.builder()
                .videoId(videoId)
                .title(title)
                .channelTitle(channelTitle)
                .viewCount(viewCount)
                .likeCount(likeCount)
                .commentCount(commentCount)
                .videoUrl("https://www.youtube.com/watch?v=" + videoId)
                .isShorts(isShorts)
                .build();
    }
}
