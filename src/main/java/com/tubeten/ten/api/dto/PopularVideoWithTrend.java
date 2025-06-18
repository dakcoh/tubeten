package com.tubeten.ten.api.dto;

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
                .build();
    }
}
