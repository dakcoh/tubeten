package com.tubeten.ten.domain;

import com.tubeten.ten.api.popularvideo.dto.PopularVideoResponse;

public record PopularVideoWithTrend(
        String videoId, String title, String channelTitle,
        long viewCount, long likeCount, long commentCount,
        String videoUrl, boolean isShorts, String trend, Integer rankDiff
) {
    public static PopularVideoWithTrend of(PopularVideoResponse v, String trend) {
        return new PopularVideoWithTrend(
                v.videoId(), v.title(), v.channelTitle(), v.viewCount(), v.likeCount(),
                v.commentCount(), v.videoUrl(), v.shorts(), trend, null
        );
    }
    public static PopularVideoWithTrend of(PopularVideoResponse v, String trend, Integer diff) {
        return new PopularVideoWithTrend(
                v.videoId(), v.title(), v.channelTitle(), v.viewCount(), v.likeCount(),
                v.commentCount(), v.videoUrl(), v.shorts(), trend, diff
        );
    }
}