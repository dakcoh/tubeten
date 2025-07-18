package com.tubeten.ten.init;

import com.tubeten.ten.batch.PopularVideoScheduler;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PopularVideoInitializer {

    private final PopularVideoScheduler scheduler;

    @PostConstruct
    public void init() {
        try {
            scheduler.fetchAndCacheTop100(); // ✅ 앱 시작 시 1회 실행
        } catch (Exception e) {
            // Redis가 연결 안되어 있어도 앱은 살아 있어야 함
            System.out.println("⚠️ 앱 시작 시 인기 영상 캐싱 실패 (무시): " + e.getMessage());
        }
    }
}