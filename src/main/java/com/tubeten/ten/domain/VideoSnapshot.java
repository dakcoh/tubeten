package com.tubeten.ten.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "video_snapshot")
public class VideoSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String videoId;
    private String title;
    private String channelTitle;

    private int rank;
    private long viewCount;

    private String regionCode;

    private String categoryId;

    private LocalDateTime snapshotTime;
}
