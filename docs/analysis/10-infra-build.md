# 10. 인프라 / 빌드 / 배포

## 개요

HonBam-api는 Spring Boot 2.7.17 모놀리식 애플리케이션으로, Gradle로 빌드하고 Docker(멀티스테이지)로 패키징한다. 런타임 의존 인프라는 MySQL 8 / Redis / RabbitMQ이며, 미디어 스토리지로 AWS S3(presigned URL)를 사용한다. 실행에 필요한 민감 설정(`application.yml`, `.env`)은 `.gitignore`로 제외되어 리포지토리에 없다.

## 빌드 (`build.gradle`, `settings.gradle`)

- 플러그인: `org.springframework.boot 2.7.17`, `io.spring.dependency-management 1.0.15`
- `sourceCompatibility = '11'` (Java 11 소스), rootProject = `HonBam`
- 주요 의존성

| 분류 | 의존성 |
|------|--------|
| 웹/JPA | `spring-boot-starter-web`, `-data-jpa`, `-validation`, `-webflux`(WebClient) |
| 보안/인증 | `-security`, `-oauth2-client`, jjwt 0.11.2(api/impl/jackson) |
| 실시간/메시징 | `-websocket`, `-data-redis`, `-cache`, `-amqp`(RabbitMQ) |
| 스토리지 | `software.amazon.awssdk:s3:2.25.26`, `io.minio:minio:8.5.1` |
| 문서/로깅 | `springdoc-openapi-ui 1.6.9`(Swagger), `p6spy-spring-boot-starter 1.8.0` |
| DB | `com.mysql:mysql-connector-j` |
| 유틸 | commons-io, commons-codec 1.15, json-simple, jackson-databind |
| 테스트 | `-starter-test`, Testcontainers 1.19.7 (core/junit-jupiter/mysql) |
| 개발 | lombok, devtools, configuration-processor |

> **발견 (PLAUSIBLE)**: `io.minio:minio:8.5.1`이 선언돼 있으나 실제 코드는 AWS S3 SDK만 사용한다(미사용 의존성). 상세는 `08-upload-media.md` 참조.
> **발견 (CONFIRMED)**: `-amqp`(RabbitMQ) 의존성과 docker-compose의 RabbitMQ 컨테이너가 있으나 실제 AMQP/STOMP relay 코드는 없다. 상세는 `02-chat-realtime.md` 참조.

## Docker (`Dockerfile`)

멀티스테이지 빌드:
1. **빌드 스테이지**: `eclipse-temurin:17-jdk` — `gradlew clean build -x test --no-daemon`(테스트 제외), CRLF 제거(`sed -i 's/\r$//' gradlew`)
2. **런타임 스테이지**: `eclipse-temurin:17-jre` — 산출 jar 실행

> **발견 (CONFIRMED)**: 빌드/런타임은 **JDK 17**인데 `build.gradle`의 `sourceCompatibility`는 **11**이다. 동작에는 문제없으나(11 바이트코드를 17 JRE가 실행) 툴체인/타깃 버전이 일치하지 않는다.

## Docker Compose (`docker-compose.yml`)

| 서비스 | 이미지 | 포트(host→container) | 비고 |
|--------|--------|----------------------|------|
| mysql | mysql:8.0 | 3307→3306 | utf8mb4, `--secure-file-priv`, `--local-infile=1`(CSV LOAD), `./init`→initdb.d, `./seed`→mysql-files, healthcheck, TZ Asia/Seoul |
| redis | redis:latest | 6379→6379 | refresh 토큰, ws-ticket, social token, Pub/Sub |
| rabbitmq | rabbitmq:3.13.0-management | 5672(AMQP)/15672(mgmt)/61613(STOMP)/61614(STOMP TLS) | `rabbitmq_stomp`·`rabbitmq_web_stomp` 플러그인 활성, guest/guest |

- `springboot` 앱 서비스 블록은 **주석 처리**되어 있다 → 현재 앱은 컨테이너 외부(로컬/IDE)에서 실행하고 인프라만 컨테이너로 띄우는 구성이다.
- 환경변수(`${MYSQL_*}`, `${PAYMENT_TOSS_*}`, `${KAKAO_*}`, `${SPRING_DATASOURCE_URL}`)는 `.env`(gitignore됨)에서 주입.

## DB 초기화 (`init/`, `seed/`)

- `init/01_schema.sql`: `honbam` DB에 **`tbl_recipe` 테이블만** 생성. 나머지 테이블은 JPA `ddl-auto`(application.yml 미공개, 추정)가 생성하는 구조.
- `init/02_import.sh`: 컨테이너 부팅 시 `seed/cocktail_recipe.csv`를 `LOAD DATA LOCAL INFILE`로 `tbl_recipe`에 적재. `LINES TERMINATED BY '\r\n'`(CRLF 가정), `IGNORE 1 ROWS`(헤더), `data_id = NULL`(auto increment).
- `seed/cocktail_recipe.csv`: 칵테일 시드 데이터(UTF-8 BOM, ~849행).

> **발견 (PLAUSIBLE)**: `02_import.sh`는 CSV 줄바꿈을 `\r\n`로 고정 가정한다. seed 파일이 LF로 저장되면 적재가 깨질 수 있다(스크립트 주석도 이 점을 인지). 또한 스키마는 recipe만 코드화되어 있어 나머지 도메인 테이블 스키마는 JPA 생성에 의존 → 운영 환경 DDL 관리 일원화 부재.

## 설정 키 카탈로그 (코드의 `@Value` / `@ConfigurationProperties` 전수)

`application.yml`이 리포에 없으므로, 아래는 코드가 **요구하는** 키 목록이다. 운영 시 반드시 정의되어야 한다.

### `@Value` 주입 키

| 키 | 용도 |
|----|------|
| `jwt.secret`, `jwt.base64Secret`, `jwt.issuer` | JWT 서명/검증 |
| `spring.data.redis.host`, `spring.data.redis.port` | Redis 연결 |
| `kakao.client_id`, `kakao.redirect_url`, `kakao.client_secret` | 카카오 수동 OAuth |
| `app.oauth2.redirect.success`, `app.oauth2.redirect.failure` | OAuth2 성공/실패 리다이렉트 |
| `app.rabbitmq.stomp.host/port/username/password/virtual-host` | RabbitMQ STOMP relay (⚠️ 주입만 되고 미사용 — `02-chat-realtime.md`) |
| `aws.s3.access-key`, `aws.s3.secret-key`, `aws.s3.region`, `aws.s3.bucket` | S3 presigned |
| `payment.toss.test_client_api_key`, `payment.toss.test_secret_api_key`, `payment.toss.success_url`, `payment.toss.fail_url` | Toss 결제 |
| `upload.path` | 로컬 정적 `/uploads` 저장 경로(WebConfig) |
| `websocket.ticket.ttl` | WS 티켓 Redis TTL(기본 30초) |

### `@ConfigurationProperties(prefix="auth")` — `AuthProperties`

- `auth.token.accessExpireMinutes`, `auth.token.refreshExpireDays`
- `auth.cookie.access.*`, `auth.cookie.refresh.*` (`maxAgeMinutes`/`maxAgeDays`, `httpOnly`, `secure`, `sameSite`, `path`, `domain`)
- `HonBamApplication`에 `@ConfigurationPropertiesScan` 선언

### 표준 Spring Security OAuth2 키(코드 외, 표준 파이프라인용)
- `spring.security.oauth2.client.registration.kakao|naver.*` (CustomOAuth2UserService가 naver/kakao 속성 파싱)

## 운영 관점 관찰 요약

- 설정/시크릿이 리포 밖에 있어 신규 환경 부트스트랩 시 **요구 키 문서가 사실상 이 카탈로그**가 된다.
- 인프라는 compose로 일원화돼 있으나 앱 자체는 컨테이너화 비활성 상태(주석).
- DDL 관리가 JPA 자동 생성에 의존 → 스키마 변경 추적/마이그레이션 도구(Flyway/Liquibase) 부재.
