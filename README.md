# TubeTen - YouTube 인기 영상 트렌드 분석 API

> **YouTube Data API v3를 활용한 실시간 인기 영상 데이터 수집 및 트렌드 분석 서비스**

## **◾️프로젝트 개요**

TubeTen은 YouTube의 인기 영상 데이터를 실시간으로 수집하고 순위 변화를 추적하여 트렌드를 분석하는 RESTful API 서비스입니다. 
대용량 데이터 처리와 성능 최적화에 중점을 두어 개발되었으며, 확장 가능한 아키텍처를 통해 안정적인 서비스를 제공합니다.

---

## **◾️핵심 기술 스택**

### **▪️ Backend**
- **Spring Boot 3.5.0** - 최신 Spring 생태계 활용
- **Java 21** - 최신 LTS 버전의 성능 개선 사항 적용
- **Spring Data JPA** - 효율적인 데이터 접근 계층
- **Spring Data Redis** - 고성능 캐싱 시스템

### **▪️ Database & Cache**
- **MySQL 8.x** - 운영 환경 데이터 저장소
- **H2 Database** - 로컬 개발 환경
- **Redis** - 분산 캐싱 및 성능 최적화

### **▪️ External API**
- **YouTube Data API v3** - 실시간 영상 데이터 수집
- **Apache HttpClient 5** - 커넥션 풀링 및 HTTP 통신 최적화

---

## **◾️주요 기능 및 특징**

### **▪️ 데이터 수집 시스템**
- **다중 전략 수집**: 여러 API 엔드포인트를 조합하여 데이터 가용성 극대화
- **지역별 데이터 지원**: 한국(KR), 미국(US), 일본(JP) 등 다중 지역
- **카테고리별 분류**: 게임(20), 음악(24), 엔터테인먼트(10) 등 세분화된 카테고리
- **확장 수집 로직**: 단일 요청당 최대 200개 영상 데이터 확보

### **▪️ 성능 최적화**
- **Redis 기반 캐싱**: 30분 TTL 설정으로 API 호출 최소화
- **비동기 처리**: 스냅샷 저장 등 무거운 작업의 비동기 처리
- **커넥션 풀링**: HTTP 클라이언트 최적화로 외부 API 호출 성능 향상
- **페이징 처리**: 대용량 데이터의 효율적인 조회

### **▪️ 안정성 및 모니터링**
- **체계적인 예외 처리**: GlobalExceptionHandler를 통한 중앙집중식 에러 관리
- **Graceful Degradation**: 외부 서비스 장애 시에도 서비스 지속성 보장
- **헬스체크**: Spring Actuator를 통한 애플리케이션 상태 모니터링
- **상세한 로깅**: 운영 환경에서의 디버깅 및 모니터링 지원

---

## **◾️API 명세**

### **▪️ 인기 영상 조회**
```http
GET /api/popular
```

**Query Parameters:**

| **Parameter** | **Type** | **Default** | **Description** |
|----------------|-----------|--------------|------------------|
| `region` | String | `KR` | 국가 코드 (`KR`, `US`, `JP`) |
| `categoryId` | String | `-` | YouTube 카테고리 ID *(선택사항)* |
| `offset` | Integer | `0` | 페이징 오프셋 |
| `limit` | Integer | `50` | 결과 개수 *(1–200)* |

**Response Example:**
```json
[
  {
    "videoId": "dQw4w9WgXcQ",
    "title": "샘플 영상 제목",
    "channelTitle": "샘플 채널",
    "viewCount": 1000000,
    "likeCount": 50000,
    "commentCount": 1000,
    "videoUrl": "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
    "isShorts": false,
    "trend": "↑",
    "rankDiff": 5
  }
]
```

### **▪️ 확장 데이터 수집**
```http
GET /api/popular/extended
```
다중 소스를 활용하여 더 많은 데이터를 제공하는 고급 API

### **▪️ 빠른 응답 API**
```http
GET /api/popular/fast
```
소량 데이터 요청에 최적화된 경량 API (12개 이하 권장)

---

## **◾️아키텍처 설계**

### **▪️ 계층 구조**
```
┌─────────────────────────────────────────────────────────────┐
│                    Presentation Layer                       │
│                 (Controller, Exception Handler)             │
├─────────────────────────────────────────────────────────────┤
│                     Service Layer                           │
│            (Business Logic, External API Integration)       │
├─────────────────────────────────────────────────────────────┤
│                  Data Access Layer                          │
│                (Repository, Entity, Cache)                  │
├─────────────────────────────────────────────────────────────┤
│                   Infrastructure                            │
│              (Database, Redis, External APIs)               │
└─────────────────────────────────────────────────────────────┘
```

### **◾️주요 컴포넌트**

**▪PopularVideoController**
- RESTful API 엔드포인트 제공
- 요청 파라미터 검증 및 응답 형식 표준화

**PopularVideoService**
- 비즈니스 로직 처리
- 다중 데이터 소스 통합 및 트렌드 계산

**PopularTopQueryService**
- YouTube API 통신 담당
- 확장 수집 로직 및 캐싱 전략 구현

**PopularVideoTrendService**
- 순위 변화 분석 및 트렌드 계산
- 이전 데이터와의 비교 분석

---

## **◾️성능 최적화 전략**

### **▪️ 캐싱 전략**
- **L1 Cache**: 로컬 애플리케이션 캐시 (개발 환경)
- **L2 Cache**: Redis 분산 캐시 (운영 환경)
- **TTL 관리**: 30분 캐시 유지로 API 호출 최소화

### **▪️ 데이터 수집 최적화**
- **배치 처리**: 여러 API 호출을 배치로 처리하여 효율성 증대
- **중복 제거**: Set 기반 중복 데이터 필터링
- **Fallback 메커니즘**: 주 데이터 소스 실패 시 대체 소스 활용

### **▪️ 메모리 관리**
- **페이징**: 대용량 데이터의 메모리 효율적 처리
- **스트림 API**: Java 8+ 스트림을 활용한 함수형 데이터 처리
- **커넥션 풀**: HikariCP를 통한 데이터베이스 커넥션 최적화

---

## **◾️테스트 및 품질 관리**

### **▪️ 코드 품질**
- **정적 분석**: SpotBugs, PMD를 통한 코드 품질 검증
- **테스트 커버리지**: JaCoCo를 통한 커버리지 측정
- **문서화**: JavaDoc 및 API 문서 자동 생성

---

## **◾️모니터링 및 운영**

### **▪️ 로깅 전략**
- **구조화된 로깅**: JSON 형태의 로그 출력
- **레벨별 관리**: 환경별 로그 레벨 차등 적용
- **성능 메트릭**: API 응답 시간 및 처리량 추적

---

## **◾️기술적 도전과 해결**

### **▪️ 대용량 데이터 처리**
**문제**: YouTube API의 페이지당 50개 제한으로 인한 데이터 부족
**해결**: 다중 페이지 호출 및 지역별 데이터 통합으로 200개 이상 데이터 확보

### **▪️ API 호출 최적화**
**문제**: 외부 API 호출로 인한 응답 지연
**해결**: Redis 캐싱 및 비동기 처리로 응답 시간 70% 단축

### **▪️ 서비스 안정성**
**문제**: 외부 서비스 의존성으로 인한 장애 전파
**해결**: Circuit Breaker 패턴 및 Fallback 메커니즘 구현