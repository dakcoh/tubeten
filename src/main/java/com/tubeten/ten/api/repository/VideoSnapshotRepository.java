package com.tubeten.ten.api.repository;

import com.tubeten.ten.domain.VideoSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface VideoSnapshotRepository extends JpaRepository<VideoSnapshot, Long> {

    // categoryId가 null인 경우도 처리
    @Query("""
        SELECT MAX(v.snapshotTime)
        FROM VideoSnapshot v
        WHERE v.regionCode = :region
          AND (:categoryId IS NULL OR v.categoryId = :categoryId)
    """)
    LocalDateTime findLatestSnapshotTime(@Param("region") String region,
                                         @Param("categoryId") String categoryId);

    // categoryId null 대응 버전
    @Query("""
        SELECT v
        FROM VideoSnapshot v
        WHERE v.snapshotTime = :snapshotTime
          AND v.regionCode = :region
          AND (:categoryId IS NULL OR v.categoryId = :categoryId)
    """)
    List<VideoSnapshot> findBySnapshotTimeAndRegionCodeAndCategoryId(
            @Param("snapshotTime") LocalDateTime snapshotTime,
            @Param("region") String region,
            @Param("categoryId") String categoryId
    );

    // 비교용 exists (자동 비교에 사용)
    @Query("""
        SELECT COUNT(v) > 0
        FROM VideoSnapshot v
        WHERE v.regionCode = :region
          AND (:categoryId IS NULL OR v.categoryId = :categoryId)
    """)
    boolean existsByRegionCodeAndCategoryId(@Param("region") String region,
                                            @Param("categoryId") String categoryId);

    List<VideoSnapshot> findTop10ByRegionCodeAndCategoryIdOrderBySnapshotTimeDesc(String region, String categoryId);
}
