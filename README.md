# Emotion Storage Server

감정 기록, 대화, 리포트, 타임캡슐 등 사용자 감정 경험을 다루는 Mooi 백엔드 서버입니다.
Spring Boot 기반으로 REST API, 인증, 배치성 작업, WebSocket 연동, 데이터 마이그레이션 구성을 포함합니다.

## 기술 스택

- Java 17
- Spring Boot 3.4
- Spring Web
- Spring Data JPA
- Spring Security / OAuth2
- JWT
- MySQL
- PostgreSQL
- Redis
- Flyway
- Swagger(OpenAPI)
- WebSocket
- Gradle

## 주요 기능

- 소셜 로그인 및 인증 처리
- 감정 대화 및 분석
- 일간 리포트 및 캘린더 조회
- 타임캡슐 생성, 조회, 수정, 삭제
- 마이페이지, 알림, 출석 기능

## 프로젝트 구조

```text
src/main/java/com/example/emotion_storage
├── attendance
├── calendar
├── chat
├── global
├── home
├── mypage
├── notification
├── report
├── timecapsule
└── user
```

- `global`: 공통 응답, 예외 처리, 보안, 필터, 설정, 유틸리티
- `user`: 로그인, 회원가입, 사용자 인증/인가
- `chat`: 감정 대화, 분석, WebSocket 연동
- `report`: 일간 리포트 관련 기능
- `timecapsule`: 타임캡슐 도메인 기능

## 실행 환경

### 사전 요구사항

- Java 17
- Docker / Docker Compose
- `.env` 파일

### 환경 설정

기본 프로필은 `local`입니다.

- `local`: MySQL 기반 로컬 실행
- `postgres-local`: PostgreSQL 기반 로컬 실행

로컬 실행 시 예시 환경 변수:

```env
MYSQL_ROOT_PASSWORD=
MYSQL_DATABASE=
MYSQL_USER=
MYSQL_PASSWORD=
POSTGRES_DB=
POSTGRES_USER=
POSTGRES_PASSWORD=
REDIS_PORT=6380
JWT_SECRET=
ACCESS_TOKEN_EXPIRATION_MINUTES=
REFRESH_TOKEN_EXPIRATION_DAYS=
GOOGLE_CLIENT_IDS=
AI_WEBSOCKET_URL=ws://localhost:8000/ws/chat
AI_SERVER_BASE_URL=http://localhost:8000
DISCORD_ERROR_WEBHOOK_URL=
```

### 로컬 인프라 실행

```bash
docker compose -f docker-compose.local.yml up -d
```

- MySQL: `3308`
- PostgreSQL: `5433`
- Redis: `6380`

### 애플리케이션 실행

```bash
./gradlew bootRun
```

PostgreSQL 기준으로 로컬 실행하려면:

```bash
SPRING_PROFILES_ACTIVE=postgres-local ./gradlew bootRun
```

## 테스트 실행

```bash
./gradlew test
```

## API 문서

애플리케이션 실행 후 Swagger UI에서 API를 확인할 수 있습니다.

- `/swagger-ui/index.html`

## 데이터베이스 마이그레이션

Flyway가 활성화되어 있으며 마이그레이션 파일은 `src/main/resources/db/migration` 에 있습니다.

## 참고 사항

- 기본 활성 프로필은 `local`입니다.
- 테스트는 `Asia/Seoul` 타임존 기준으로 실행됩니다.
- AI 서버 및 OAuth 관련 환경 변수가 없으면 일부 기능은 정상 동작하지 않을 수 있습니다.
