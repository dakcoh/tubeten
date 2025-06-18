# 서비스 내용

## 개발 참고
본 프로젝트는 ChatGPT의 기술적 조언과 코드 예시를 참고하여 설계 및 구현되었습니다.

---

## 주요 기능

- YouTube Data API를 통해 인기 영상 **Top 10** 수집  
  (지역 `regionCode`, 카테고리 `categoryId` 기준)
- **30분마다 스케줄러 실행**으로 자동 수집
- Redis에 인기 영상 **캐시 저장**
- H2 DB에 인기 영상 **스냅샷 히스토리 저장**
- API 호출 시 **Redis 캐시 우선 조회**
- 캐시 미존재 시 **DB 또는 YouTube API로 fallback**
- **이전 순위와 비교한 트렌드 정보** 제공 (`↑`, `↓`, `→`, `new`)

---

## 기술 스택

| 항목 | 내용 |
|------|------|
| Language | Java 21 |
| Framework | Spring Boot 3.x |
| Build Tool | Gradle |
| Cache | Redis (Docker 기반) |
| Database | H2 (In-Memory) |
| External API | YouTube Data API v3 |

---

## 구조 흐름

1. **스케줄러가 30분마다** 지역/카테고리별 인기 영상 수집
2. 수집된 데이터를 **Redis 캐시 + H2 DB 스냅샷**에 저장
3. 클라이언트 API 요청 시:
    - Redis 캐시에서 우선 조회
    - 캐시가 없으면 DB 또는 YouTube API로 fallback
4. 최신 데이터와 DB의 과거 스냅샷을 **비교하여 트렌드 계산**
5. 클라이언트에 **인기 영상 + 트렌드 정보** 응답

## 테스트
### API 확인 (POSTMAN 활용)
![image](https://github.com/user-attachments/assets/85816319-6a96-4c03-8016-857be50e488b)
### 캐싱 확인 (Docker + Redis 활용)
![image](https://github.com/user-attachments/assets/efe3fbbd-1c65-4cca-853a-0117ddd08684)
- 캐싱된 데이터 조회 확인
- 30분마다 snapshot 데이터 저장 및 캐싱 확인 가능

