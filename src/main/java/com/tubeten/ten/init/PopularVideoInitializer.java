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
        // 환경 프로파일 확인
        String activeProfile = System.getProperty("spring.profiles.active", "prod");
        
        if ("local".equals(activeProfile)) {
            System.out.println("로컬 환경에서는 초기 데이터 로딩을 건너뜁니다.");
            return;
        }
        
        System.out.println("운영 환경 초기화 시작 - 인기 영상 데이터 수집 중...");
        
        try {
            scheduler.warmupAndSnapshotTop100(); // 운영 환경에서 앱 시작 시 1회 실행
            System.out.println("초기 데이터 로딩 완료");
        } catch (Exception e) {
            // 초기화 실패해도 앱은 정상 시작되어야 함
            System.out.println("초기 데이터 로딩 실패 (서비스는 정상 시작): " + e.getMessage());
        }
    }
}