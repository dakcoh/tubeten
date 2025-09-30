# 서비스 내용

## 개발 참고
본 프로젝트는 ChatGPT의 기술적 조언과 코드 예시를 참고하여 설계 및 구현되었습니다.

---
## 주요 기능
- YouTube 인기 영상 Top 10 수집 (국가 / 카테고리 기준)
- 30분마다 스케줄러 실행으로 데이터 자동 수집
- Redis에 인기 영상 캐시 저장
- MySQL에 인기 영상 스냅샷(히스토리) 저장
- API 호출 시 캐시 우선 조회, fallback으로 DB 또는 API 조회
- 이전 순위와 비교한 트렌드 계산 (↑, ↓, →, new)

---

## 기술 스택
| 항목           | 사용 기술                              |
| ------------ | ---------------------------------- |
| Language     | Java 21, JavaScript (Vue 3)        |
| Backend      | Spring Boot 3.x                    |
| Frontend     | Vue 3 + Vite + Argon Design System |
| Build Tool   | Gradle                             |
| Cache        | Redis (Docker 기반)                  |
| Database     | MySQL 8.x (Docker 기반)              |
| CI/CD        | Jenkins (Web/WAS 개별 Job 자동 배포)     |
| 배포환경         | Synology NAS + Docker Compose      |
| External API | YouTube Data API v3                |


---

## 구조 흐름
### Flow : [Browser] ▶ [Nginx (Vue)] ▶ [Spring Boot API (WAS)] ▶ [Redis] + [MySQL]
### 배포 : [Jekins] ▶ [Nginx (Vue)] or [Spring Boot API (WAS)]
- 정적 리소스는 web 컨테이너에서 Nginx가 제공
- API 요청은 was(Spring Boot) 컨테이너가 처리
- 데이터 저장/조회는 Redis 우선, 이후 MySQL 또는 YouTube API fallback

## 배포 방법 (Jenkins + NAS)
- Jenkins Job 실행 (Web / WAS 각각 개별 Job)
- 각 서비스는 Dockerfile 기반으로 이미지 빌드
- NAS 내 Docker Compose로 구성된 컨테이너 재시작
- 프론트는 Nginx 정적 서비스, 백엔드는 API 포워딩

## 개선 사항
- [x] Redis에 인기 영상 캐시 저장
- [x] 이전 순위와 비교한 트렌드 계산 (↑, ↓, →, new)
- [ ] 카테고리 별 제목 및 헤쉬태그 그래프 시각화
- [ ] DNS 설정 - 라우터 설정에서 외부 포트를 내부 포트로 직접 연결
- [ ] 채널 성장 분석 및 그래프 시각화

---
## 테스트 확인
### batch 실행
<img width="843" height="379" alt="image" src="https://github.com/user-attachments/assets/0681cf9a-c2aa-4070-a3a4-43beed38ed39" />

- 실시간 순위 데이터 저장
- 30분마다 redis 캐싱
### Redis
<img width="732" height="345" alt="image" src="https://github.com/user-attachments/assets/9a3cbdad-8f4f-4842-833f-5f51cfe45316" />

- 캐싱된 데이터 조회 확인
- 30분마다 snapshot 데이터 저장 및 캐싱 확인 가능

