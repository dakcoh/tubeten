package com.tubeten.ten.domain;

import com.tubeten.ten.api.dto.PopularVideoResponse;
import lombok.*;

@Builder
@Getter
@AllArgsConstructor
public class PopularVideoWithTrend {

    private String videoId;
    private String title;
    private String channelTitle;
    private long viewCount;
    private long likeCount;
    private long commentCount;
    private String videoUrl;
    private boolean isShorts;
    private String trend;
    private Integer rankDiff;  // ✅ 추가: 순위 변화량

    // 기본 of: rankDiff 없음
    public static PopularVideoWithTrend of(PopularVideoResponse video, String trend) {
        return PopularVideoWithTrend.builder()
                .videoId(video.getVideoId())
                .title(video.getTitle())
                .channelTitle(video.getChannelTitle())
                .viewCount(video.getViewCount())
                .likeCount(video.getLikeCount())
                .commentCount(video.getCommentCount())
                .videoUrl(video.getVideoUrl())
                .isShorts(video.isShorts())
                .trend(trend)
                .rankDiff(null)
                .build();
    }

    // ✅ 확장 of: rankDiff 포함
    public static PopularVideoWithTrend of(PopularVideoResponse video, String trend, Integer rankDiff) {
        return PopularVideoWithTrend.builder()
                .videoId(video.getVideoId())
                .title(video.getTitle())
                .channelTitle(video.getChannelTitle())
                .viewCount(video.getViewCount())
                .likeCount(video.getLikeCount())
                .commentCount(video.getCommentCount())
                .videoUrl(video.getVideoUrl())
                .isShorts(video.isShorts())
                .trend(trend)
                .rankDiff(rankDiff)
                .build();
    }
}
