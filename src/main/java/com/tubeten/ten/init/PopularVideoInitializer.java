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
        scheduler.fetchAndCacheTop10(); // ✅ 앱 시작 시 1회 실행
    }
}