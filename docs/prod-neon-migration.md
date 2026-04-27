# Prod Neon Migration

## 배경

이 프로젝트는 기존의 AWS 기반 다중 환경 배포 구조에서, 운영 비용을 줄인 단일 프로덕션 구조로 전환하려고 합니다.  
동시에 기존 구조도 포트폴리오 관점에서 의미가 있으므로, 완전히 삭제하지 않고 이전 구조와 개선 구조를 함께 남기는 방향으로 정리합니다.

목표는 아래와 같습니다.

- 기존 `AWS/staging` 구조를 `legacy architecture`로 문서화하여 보존
- 운영 환경은 `prod` 하나만 유지
- AWS RDS MySQL을 `Neon Postgres`로 전환
- 백엔드와 AI 서버는 작은 VPS 한 대에서 함께 운영
- 월 고정 비용 최소화

## 아키텍처

### 기존 구조 (`as-is`)

- GitHub Actions 기반 빌드 산출물 생성
- AWS `S3 + CodeDeploy` 배포
- EC2 스타일 서버 배포 스크립트 사용
- AWS RDS MySQL 사용
- Redis 사용
- `staging / production` 분리 운영

관련 legacy 파일:

- `.github/workflows/ci-production.yml`
- `.github/workflows/deploy-production-manual.yml`
- `.github/workflows/deploy-staging-auto.yml`
- `.github/workflows/deploy-staging-manual.yml`
- `.github/workflows/rollback-production.yml`
- `.github/workflows/rollback-staging.yml`
- `scripts/deploy.sh`
- `src/main/resources/application-staging.yml`

### 목표 구조 (`to-be`)

- VPS 1대에서 `backend + redis` 운영
- AI 서버는 같은 VPS에서 수동 실행
- 주 데이터베이스는 `Neon Postgres`
- 1차 마이그레이션에서는 Redis 유지
- 배포 흐름은 `prod`만 유지
- 배포 방식은 `Docker Compose` 또는 `SSH 기반`으로 단순화
- 초기 운영은 도메인 없이 `서버 공인 IP:8080`으로 직접 접근

권장 서비스 구성:

- `backend`
- `redis`
- `ai-server`(수동 실행)

권장 환경 변수 예시:

```env
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:postgresql://<neon-host>/<db>?sslmode=require
DB_USERNAME=<neon-user>
DB_PASSWORD=<neon-password>
DB_DRIVER_CLASS_NAME=org.postgresql.Driver
HIBERNATE_DIALECT=org.hibernate.dialect.PostgreSQLDialect
AI_SERVER_BASE_URL=http://host.docker.internal:8000
AI_WEBSOCKET_URL=ws://host.docker.internal:8000/ws/chat
```

## 마이그레이션 단계

### 1단계. 코드베이스 준비

- 기존 AWS 관련 파일은 바로 삭제하지 않고 유지
- Postgres 런타임 지원 추가
- 운영 설정을 provider-agnostic 한 `DB_*` 변수 기준으로 전환
- 목표 구조와 전환 순서를 문서화

### 2단계. Flyway 및 JPA를 Postgres 기준으로 정리

- 모든 Flyway SQL에서 MySQL 전용 문법 점검
- MySQL dump 스타일 DDL을 PostgreSQL 호환 문법으로 전환
- boolean, timestamp, identity generation 차이 점검

### 3단계. 신규 운영 환경 준비

- 저가 VPS 1대 준비
- Docker 및 Docker Compose 설치
- `backend`, `redis`, `reverse-proxy` 구동
- AI 서버는 같은 VPS에서 별도 프로세스로 수동 실행
- 백엔드가 Neon에 SSL로 연결되도록 설정

운영용 초안 파일:

- `docker-compose.prod.yml`
- `deploy/.env.prod.example`

예상 실행 방식:

```bash
cp deploy/.env.prod.example deploy/.env.prod
docker compose --env-file deploy/.env.prod -f docker-compose.prod.yml up -d --build
```

`deploy/.env.prod`에서 꼭 채워야 하는 값:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `AI_SERVER_BASE_URL`
- `AI_WEBSOCKET_URL`

운영 기본안에서는 AI 서버를 Docker Compose에 포함하지 않고, VPS 안에서 수동 실행합니다.

### 4단계. 데이터 이전

- 기존 MySQL 데이터 export
- MySQL/Postgres 차이에 맞게 스키마 및 데이터 변환
- Neon으로 import
- Neon 연결 기준으로 운영 검증 수행

### 5단계. 트래픽 전환

- VPS 기준 운영 스택 배포
- AI 서버 수동 실행 후 내부 포트 확인
- 로그인, 토큰 재발급, AI 호출, 리포트 생성, WebSocket 흐름 검증
- 초기에는 `http://서버공인IP:8080`으로 직접 검증
- 도메인이 생기면 그때 리버스 프록시와 HTTPS 추가
- 짧은 롤백 기간 동안 AWS 자원은 즉시 삭제하지 않고 유지

## 리스크 체크리스트

가장 큰 리스크는 데이터베이스 마이그레이션입니다.

우선 확인해야 할 항목:

- `AUTO_INCREMENT` 컬럼 처리
- `tinyint(1)`의 boolean 변환
- `datetime` 및 timezone 처리
- MySQL 전용 `ALTER TABLE ... MODIFY COLUMN`
- MySQL 전용 `UPDATE ... JOIN`
- nullable 컬럼이 포함된 unique index 동작 차이

현재 Postgres 호환성 검토가 필요한 Flyway 파일:

- `src/main/resources/db/migration/V1__init_schema.sql`
- `src/main/resources/db/migration/V2__alter_users_email_nullable.sql`
- `src/main/resources/db/migration/V3__add_user_id_to_reports.sql`
- `src/main/resources/db/migration/V4__reports_user_id_not_null.sql`
