# HonBam API Server


HonBam은 홈텐딩(Home-tending) 문화를 즐기는 사용자들이 모여 소통하고 실시간으로 대화할 수 있는 소셜 플랫폼의 **API 서버**입니다.

---

## Summary

- **주요 도메인**: 인증/계정, SNS(게시글/댓글/좋아요/팔로우), 레시피, 실시간 채팅(WebSocket), 미디어 업로드, 결제(Toss)
- **핵심 설계 포인트**: WebSocket 인증 분리(티켓), 이벤트 기반 비동기 처리, Redis Pub/Sub 기반 확장성
- **운영 관점 고려**: 메시지 저장 트랜잭션 최소화 및 동시성 이슈 대응, 미디어 업로드 Presigned URL 적용

---

## Highlights

### 1) WebSocket 인증: JWT를 WS에 직접 사용하지 않고 “티켓 + Redis(TTL)”로 분리
- HTTP 인증 이후 **WS Ticket 발급 → Redis 저장(TTL) → CONNECT 시 1회 검증**으로 세션 컨텍스트를 구성
- 재연결/만료/폐기 정책을 단순화하고, WebSocket 핸드셰이크에서 인증 로직을 가볍게 유지

### 2) 메시지 저장과 부가 처리를 분리: 이벤트 기반 비동기 처리
- 핵심 경로는 **메시지 INSERT 중심**으로 최소화
- 브로드캐스팅/알림/안 읽은 수 갱신 등은 **Spring Event + `@Async`**로 분리하여 응답 지연을 줄이고 확장성을 확보

### 3) 실시간 확장성: Redis Pub/Sub으로 멀티 인스턴스 동기화
- 서버 인스턴스가 여러 대로 확장되더라도 **read-state / broadcast 이벤트**를 Pub/Sub으로 동기화
- 실시간 채팅의 **서버 간 상태 불일치**를 줄이는 구조

### 4) 동시성 이슈 대응: 채팅방 lastMessage 갱신 경합 완화
- 동시 전송 시 동일 채팅방 row 갱신 경합으로 **lock wait timeout / 정합성 문제**가 발생할 수 있음
- 저장 트랜잭션 범위를 줄이고, 부가 작업을 비동기 처리로 분리하는 방향으로 개선(설계/구현 고도화)

---

## Core Features

### 사용자 / 인증
- 로컬 회원가입(Email/Password) 및 **JWT 기반 인증**
- 소셜 로그인(Kakao, Naver 등 OAuth2)
- 사용자 프로필 관리

### SNS
- 게시물(Post) CRUD
- 좋아요(Like), 댓글(Comment)
- 팔로우(Follow/Following)

### 실시간 채팅 (Real-time Chat)
- WebSocket(STOMP) 기반 실시간 통신
- **1:1 / 그룹 / 오픈 채팅** 지원
- 읽음 처리 및 미읽음 수 실시간 업데이트
- 이미지/파일 전송

### 미디어 처리
- 프로필/게시물/채팅 메시지 이미지/파일 업로드
- **AWS S3 + Presigned URL** 지원

### 결제
- Toss Payments 연동을 통한 구독 또는 포인트 결제 기능

---

## Tech Stack & Architecture

- **Backend**: Java 11, Spring Boot 2.7+
- **Database**: JPA(Hibernate), RDBMS(MySQL)
- **Authentication**: Spring Security, JWT, OAuth2
- **Real-time**: Spring WebSocket, STOMP
- **Cache / Messaging**: Redis (WS 인증 티켓(TTL), Pub/Sub 이벤트 동기화, 캐싱)
- **File Storage**: AWS S3 
- **Payments**: Toss Payments API
- **Build**: Gradle
- **Container**: Docker

### Architecture Notes
- 기능 단위(`userapi`, `chatapi`, `snsapi` …)로 패키지를 분리해 **응집도/결합도**를 관리합니다.
- 메시지 전송 이후의 부가 작업은 이벤트로 분리하여 **핵심 트랜잭션을 가볍게 유지**합니다.
- Redis Pub/Sub 기반으로 실시간 이벤트를 동기화해 **수평 확장**을 고려했습니다.

---

## Module Structure

하위 모듈 책임은 다음과 같습니다.

| 모듈명 | 설명 |
|---|---|
| `auth` | 로컬(JWT) 및 소셜(OAuth2) 로그인, 토큰 발급/검증 등 인증/인가 |
| `userapi` | 사용자 정보 조회, 가입, 프로필 수정 등 계정 관련 API |
| `snsapi` | 게시물/댓글/좋아요/팔로우 등 SNS 기능 |
| `recipeapi` | 칵테일 레시피 조회/등록 등 레시피 기능 |
| `chatapi` | WebSocket/STOMP 기반 실시간 채팅 관련 API 및 서비스 |
| `paymentsapi` | Toss Payments 연동 결제 관련 API 및 서비스 |
| `upload` | S3/MinIO 파일 업로드 및 Presigned URL 발급 |
| `notification` | 푸시 알림 등 사용자 알림 로직 |
| `config` | Security/Redis 등 주요 설정 |
| `exception` | GlobalExceptionHandler 및 커스텀 예외 |
| `filter`, `interceptor` | HTTP/WebSocket 요청 전처리(`JwtAuthFilter` 등) |

---

## Getting Started

### Prerequisites
- Java 11
- Gradle 7.x
- Docker / Docker Compose (Redis, Mysql)

### Build & Run

```bash
# build
./gradlew build

# run
./gradlew bootRun
