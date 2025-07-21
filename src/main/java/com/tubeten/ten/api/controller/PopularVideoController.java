package com.tubeten.ten.api.controller;

import com.tubeten.ten.domain.PopularVideoWithTrend;
import com.tubeten.ten.api.service.PopularVideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class PopularVideoController {

    private final PopularVideoService popularVideoService;

    @GetMapping("/popular")
    public ResponseEntity<List<PopularVideoWithTrend>> getPopularVideos(
            @RequestParam(defaultValue = "KR") String region,
            @RequestParam(required = false) String categoryId,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "6") int limit
    ) {
        return ResponseEntity.ok(popularVideoService.getPopularVideosWithAutoTrend(region, categoryId, offset, limit));
    }
}
