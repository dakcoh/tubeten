package com.tubeten.ten.api.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PopularVideoResponse(
        String videoId,
        String title,
        String channelTitle,
        long viewCount,
        long likeCount,
        long commentCount,
        String videoUrl,
        @JsonAlias("isShorts") boolean shorts
) {
    public static PopularVideoResponse from(JsonNode item) {
        JsonNode snippet = item.path("snippet");
        JsonNode statistics = item.path("statistics");

        String videoId = item.path("id").asText("");
        String title = snippet.path("title").asText("");
        String channelTitle = snippet.path("channelTitle").asText("");

        long viewCount = statistics.path("viewCount").asLong(0L);
        long likeCount = statistics.path("likeCount").asLong(0L);
        long commentCount = statistics.path("commentCount").asLong(0L);

        boolean shorts = detectShorts(snippet, title);
        String videoUrl = videoId.isEmpty() ? null : "https://www.youtube.com/watch?v=" + videoId;

        return new PopularVideoResponse(
                videoId, title, channelTitle, viewCount, likeCount, commentCount, videoUrl, shorts
        );
    }

    private static boolean detectShorts(JsonNode snippet, String title) {
        if (snippet.has("tags")) {
            for (JsonNode tag : snippet.get("tags")) {
                if (tag.asText().toLowerCase().contains("shorts")) return true;
            }
        }
        String t = title == null ? "" : title.toLowerCase();
        return t.contains("shorts") || t.contains("[shorts]");
    }
}
