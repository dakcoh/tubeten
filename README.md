<p align="center">
  <img src="./assets/logo.png" alt="TubeTen Logo" width="180"/>
</p>

<h1 align="center">TubeTen</h1>

<p align="center">
  YouTube 공개 데이터의 변화량을 분석해 성장 중인 영상과 채널을 보여주는 트렌드 서비스
</p>

<p align="center">
  Java·Spring Boot 기반의 데이터 수집·배치·조회 API를 직접 설계하고 운영한 개인 프로젝트
</p>

<p align="center">
  <a href="https://www.tubeten.co.kr"><strong>Live Service</strong></a>
  &nbsp;·&nbsp;
  <a href="https://www.tubeten.co.kr/api/swagger-ui.html"><strong>API Docs</strong></a>
  &nbsp;·&nbsp;
  <a href="https://www.tubeten.co.kr/api/docs"><strong>OpenAPI JSON</strong></a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-orange.svg" alt="Java 21">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.5-brightgreen.svg" alt="Spring Boot 3.5">
  <img src="https://img.shields.io/badge/Nuxt-3.21-00DC82.svg" alt="Nuxt 3.21">
  <img src="https://img.shields.io/badge/MySQL-8.0-blue.svg" alt="MySQL 8">
  <img src="https://img.shields.io/badge/Redis-7-red.svg" alt="Redis 7">
  <img src="https://img.shields.io/badge/OpenAPI-3.1-6BA539.svg" alt="OpenAPI 3.1">
</p>

---

## 프로젝트 소개

TubeTen은 단순 누적 조회수보다 **최근 조회수·좋아요·댓글의 변화량**을 중심으로 YouTube 콘텐츠의 성장 흐름을 보여주는 서비스입니다.

한국·미국·일본의 인기 영상과 크리에이터 데이터를 주기적으로 수집하고 다음 기능을 제공합니다.

- 국가·카테고리별 실시간 인기 영상
- 트렌드 대시보드와 Shorts 분석
- 영상 상세 성장 추이
- 크리에이터 검색과 성장 리포트
- 채널 성장률 비교
- 주간 트렌드 리포트

> YouTube의 공식 순위를 복제하는 서비스가 아니라, YouTube Data API의 공개 데이터를 자체적으로 수집하고 변화량을 계산해 보여주는 프로젝트입니다.

## 경력기술서 요약

| 항목 | 내용 |
|---|---|
| 프로젝트 | YouTube 공개 데이터 기반 트렌드 분석 서비스 개발·운영 |
| 기간·인원 | 2026.01 ~ 현재 · 개인 프로젝트 |
| 주요 역할 | Spring Boot API, 수집·집계 배치, MySQL 데이터 모델과 운영 환경 구축 |
| 핵심 과제 | 외부 API 수집 속도 개선, DB 포화 방지, 대용량 시계열 조회 최적화, 배치 신뢰성 확보 |
| 운영 방식 | Docker Compose 기반 실제 서비스 운영, 로그·배치 이력·DB 지표를 확인하며 개선 |

한 문장으로 설명하면, **외부 API 데이터를 주기적으로 수집하고 변화량을 계산해 서비스하는 과정에서 발생한 속도와 DB 부하 문제를 직접 개선한 백엔드 중심 프로젝트**입니다.

## 직접 확인할 수 있는 결과물

| 결과물 | 링크 |
|---|---|
| 운영 서비스 | [www.tubeten.co.kr](https://www.tubeten.co.kr) |
| 인기 영상 순위 | [YouTube Top 10](https://www.tubeten.co.kr/youtube-top10) |
| API 문서 | [Swagger UI](https://www.tubeten.co.kr/api/swagger-ui.html) |
| OpenAPI 명세 | [OpenAPI JSON](https://www.tubeten.co.kr/api/docs) |

## 담당한 작업

개인 프로젝트로 시작해 백엔드를 중심으로 기능을 구현하고 실제 운영 환경에서 발생한 문제를 반복적으로 개선했습니다.

| 영역 | 수행 내용 |
|---|---|
| 데이터 수집 | YouTube API 영상·채널 수집과 30분 단위 원본 스냅샷 저장 |
| 랭킹 | 최근 변화량을 이용한 성장 점수 계산과 60분 단위 순위 발행 |
| API | 랭킹, 영상 분석, 크리에이터, 대시보드 조회 API 구현 |
| 데이터베이스 | MySQL 인덱스, 일별 파티션, 보관 기간과 정리 배치 적용 |
| 캐시 | Redis와 사전 생성 JSON을 이용한 반복 조회 부하 감소 |
| 운영 | Docker Compose 배포, health check, 로그·배치 이력 확인 |
| 프론트엔드 | Nuxt SSR 화면과 백엔드 API 연결, 기본 SEO와 반응형 화면 구성 |

프론트엔드와 인프라도 직접 구성했지만, 핵심 역할은 **Spring 기반 데이터 수집·배치·조회 API와 운영 중 발견한 병목 개선**입니다.

## 시스템 구성

```mermaid
flowchart LR
    User["사용자"] --> Nginx["Nginx"]
    Nginx --> Nuxt["Nuxt SSR"]
    Nginx --> API["Spring API"]
    API --> MySQL[("MySQL")]
    API --> Redis[("Redis")]
    Batch["Batch App"] --> YouTube["YouTube Data API"]
    Batch --> MySQL
    Batch --> Redis
```

백엔드는 공통 도메인, API, Batch 모듈로 나눴습니다.

```text
tubeten-common  도메인, 저장소, 공통 서비스
tubeten-api     사용자·관리자 API, 인증, OpenAPI
tubeten-batch   데이터 수집, 랭킹 집계, 데이터 정리
```

API와 Batch를 별도 프로세스로 실행했습니다. 사용자의 조회 요청과 무거운 데이터 수집 작업이 같은 애플리케이션의 스레드와 메모리를 직접 경쟁하지 않게 하기 위한 선택입니다.

## 데이터 처리 흐름

```text
YouTube Data API
  → 수집 대상 영상
  → 영상 스냅샷
  → 변화량 계산
  → 국가·카테고리별 랭킹
  → Redis·대시보드 JSON 캐시
  → REST API
  → Nuxt 화면
```

조회수·반응 원본은 변화량 계산의 정밀도를 위해 30분마다 저장합니다. DB 부하가 큰 수집 대상 탐색과 랭킹 발행은 60분마다 실행해 조회 서비스와의 경합을 줄였습니다. 오래된 원본은 테이블별 보관 기간과 날짜 파티션 정책에 따라 정리합니다.

## 운영하며 개선한 사례

### 1. 영상 수집 시간을 약 9분에서 35초 수준으로 줄였습니다

초기에는 YouTube API 응답을 순차적으로 기다려 영상 스냅샷 수집에 약 9분이 걸렸습니다.

Java 21 Virtual Thread를 적용해 여러 외부 API 요청을 효율적으로 기다리도록 개선했습니다. 다만 요청을 무제한으로 실행하면 YouTube API와 DB가 다시 포화될 수 있으므로, DB 저장과 외부 API 진입 수는 별도의 제한값으로 관리했습니다.

운영 환경에서 약 790개 영상의 수집 시간이 **9분 24초에서 약 35초 수준**으로 줄었습니다. 가상 스레드로 대기 비용을 줄이면서 실제 처리량은 DB 커넥션과 외부 API 한도에 맞춰 별도로 제한했습니다. 이 수치는 당시 운영 데이터와 외부 API 상태에서 관측한 값입니다.

### 2. 긴 트랜잭션으로 인한 DB 커넥션 점유를 줄였습니다

외부 API 호출을 포함한 전체 메서드에 트랜잭션이 걸려 있어, 네트워크 응답을 기다리는 동안에도 DB 커넥션을 오래 점유할 수 있었습니다.

외부 API 호출과 작업 조율에서는 긴 트랜잭션을 제거하고, 실제 저장 구간에만 짧은 트랜잭션을 적용했습니다. 여러 건의 저장은 batch UPSERT로 합쳐 DB 왕복 횟수도 줄였습니다.

핵심은 **네트워크 작업과 DB 저장 작업의 경계를 분리한 것**입니다. 외부 API가 느려져도 DB 커넥션이 함께 묶여 있는 시간을 줄였습니다.

### 3. 데이터가 늘어나도 필요한 범위만 조회하도록 개선했습니다

랭킹과 영상 스냅샷이 계속 누적되면서 일부 집계 쿼리가 오래된 데이터까지 읽는 문제가 있었습니다.

- 조회에 필요한 최근 시간 범위를 SQL에 명시
- 실행 계획을 확인하고 실제 조회 조건에 필요한 복합 인덱스만 적용
- 날짜 단위 RANGE 파티션 사용
- 만료 데이터는 파티션 단위로 정리
- 대시보드와 Shorts 응답은 미리 생성해 저장

Shorts 시계열과 히트맵 조회는 운영 데이터 약 44만 행 기준으로 각각 약 **51ms, 47ms**가 관측됐습니다. 인덱스를 계속 추가한 것이 아니라 조회 조건과 중복 여부를 확인했고, 필요하지 않은 인덱스는 Flyway 변경 이력으로 제거했습니다. 측정값은 데이터량과 캐시 상태에 따라 달라질 수 있습니다.

### 4. 배치가 실제로 끝났는지 확인할 수 있게 만들었습니다

스케줄러 로그만으로는 재시작이나 timeout 이후 작업이 실제로 끝났는지 판단하기 어려웠습니다.

작업 시작·완료·실패 상태와 처리 건수, 실행 시간을 DB에 기록했습니다. 같은 작업이 여러 Batch 인스턴스에서 동시에 실행되지 않도록 작업 소유자와 **소유권 유효 시간(lease)**도 저장했습니다. 덕분에 로그만 보는 대신 DB 이력과 로그를 함께 사용해 실행 결과를 판단할 수 있습니다.

## 핵심 기술 의사결정

- **왜 API와 Batch를 분리했나요?** 사용자 조회와 무거운 수집·집계가 같은 실행 자원을 경쟁하지 않게 하기 위해서입니다.
- **왜 모든 작업을 병렬화하지 않았나요?** 가상 스레드는 대기를 가볍게 하지만 DB 커넥션과 외부 API 한도를 늘려주지는 않기 때문입니다.
- **왜 스냅샷은 30분이고 랭킹은 60분인가요?** 변화량 계산용 원본 정밀도는 유지하면서, 무거운 집계와 캐시 갱신 횟수를 줄여 DB 포화를 완화하기 위해서입니다.
- **왜 인덱스를 계속 추가하지 않았나요?** 인덱스도 저장·갱신 비용이 있으므로 실행 계획과 실제 조건을 확인해 필요한 것만 유지하고 중복 인덱스는 제거했습니다.
- **성능 개선을 어떻게 검증했나요?** 실행 시간, 조회 행 수, DB 커넥션 사용량, 배치 성공 이력과 회귀 테스트를 함께 확인했습니다.

## API 문서와 호환성

[Swagger UI](https://www.tubeten.co.kr/api/swagger-ui.html)에서 운영 중인 공개 API의 요청 파라미터와 응답 모델을 확인할 수 있습니다.

- OpenAPI 3.1 사용
- 운영 서비스와 같은 도메인에서 Swagger UI 제공
- 관리자·내부 처리 API는 공개 문서에서 제외
- 정적 `swagger.yaml`은 Redocly CLI로 구조 검증
- `master` push와 pull request에서 GitHub Actions 자동 실행
- 기존 offset 방식과 새로운 snapshot cursor 방식을 함께 지원

snapshot cursor는 첫 페이지의 데이터 기준 시각과 마지막 순위를 다음 요청에 전달합니다. 페이지를 넘기는 도중 새 랭킹이 발행돼도 처음 보던 시점의 목록을 이어서 제공하기 위한 방식입니다.

## 운영 방식

| 상황 | 대응 방식 |
|---|---|
| YouTube API 일시 오류 | Retry와 CircuitBreaker 적용, 일부 영상 누락은 부분 성공으로 기록 |
| Redis 장애 | 캐시 오류를 기록하고 DB 조회로 fallback |
| API 배포 | 2개 API 컨테이너를 순차 교체하고 health 확인 |
| Batch 배포 | Flyway 이력과 배치 로그 확인 후 단일 컨테이너 교체 |
| 데이터 증가 | 보관 기간과 파티션 정리 상태 확인 |
| 조회 지연 | 실행 계획, 조회 행 수, 인덱스 사용 여부를 함께 확인 |

운영 설정을 무조건 크게 잡기보다 API 응답 시간, DB 커넥션 사용량, 배치 단계별 실행 시간을 확인한 뒤 조정하는 것을 원칙으로 삼았습니다.

## 기술 스택

| 분류 | 기술 |
|---|---|
| Backend | Java 21, Spring Boot 3.5, Spring Data JPA, QueryDSL, JdbcTemplate |
| Batch | Spring Scheduling, Virtual Thread, Flyway |
| Database | MySQL 8, Redis 7 |
| Resilience | Resilience4j Retry, CircuitBreaker |
| Frontend | Nuxt 3, Vue 3, Pinia, ECharts |
| Infra | Docker Compose, Nginx |
| API Contract | OpenAPI 3.1, Swagger UI, Redocly CLI |

## 검증

| 검증 항목 | 결과 |
|---|---|
| Backend | JUnit 5 — 47 suites, 106 tests, 실패 0 |
| Docker integration | MySQL 8·Redis 7·활성 Batch 14개 — 3 suites, 10 tests, 실패 0 |
| Spring context | API·Batch test profile 기동 확인 |
| Frontend | ESLint, vue-tsc typecheck 통과 |
| Production build | API·Batch와 Nuxt production build 통과 |
| OpenAPI | Redocly CLI 구조 검증과 GitHub Actions 통과 |
| UI regression | Playwright 177개 프로젝트 케이스 중 105개 통과, viewport 중복 72개 의도적 제외, 실패 0 |

테스트 환경은 운영 MySQL과 완전히 같지 않습니다. 따라서 자동 테스트 통과와 운영 성능 보장을 같은 의미로 보지 않고, 배포 전후 Flyway 이력, 실제 인덱스·파티션, 컨테이너 health와 로그를 별도로 확인합니다.

## 이 프로젝트를 통해 배운 점

- 외부 API 병렬 처리와 DB 동시성은 별도로 제한해야 한다는 점
- 트랜잭션 범위가 DB 커넥션 사용 시간에 직접 영향을 준다는 점
- 인덱스는 개수보다 실제 쿼리 조건과 실행 계획이 중요하다는 점
- 배치는 성공 여부뿐 아니라 중복 실행과 timeout 이후 상태까지 기록해야 한다는 점
- 성능 수치는 측정 시점의 데이터량과 조건을 함께 밝혀야 한다는 점
- 배포 후 확인 절차와 rollback 경로도 기능 구현의 일부라는 점

---

<p align="center">
  <strong>프로젝트 기간</strong>: 2026.01 ~ 현재
  &nbsp;·&nbsp;
  <strong>역할</strong>: 백엔드 중심 개인 프로젝트
  &nbsp;·&nbsp;
  <strong>업데이트</strong>: 2026-07-23
</p>
