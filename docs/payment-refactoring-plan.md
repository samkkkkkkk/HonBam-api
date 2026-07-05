# Payment 도메인(`paymentsapi`) 리팩토링 계획

## Context

`refactor/payment` 브랜치(현재 development와 동일, 작업 시작 전)에서 결제 도메인을 리팩토링한다. 현재 `TossService`는 클래스 레벨 `@Transactional` 아래에서 외부 Toss API 호출과 DB 저장이 뒤섞여 있고, 매 요청마다 HTTP 클라이언트를 새로 생성하며, 실제 동작하지 않는 버그(cancel의 JSON 본문, 깨진 네이티브 SQL, NPE 경로)를 포함한다. 테스트는 0개다.

**합의된 범위**: 구조 개선 + 버그 수정 + 핵심 서비스 단위 테스트(Toss API mock). 기능 공백(webhook, 결제→PREMIUM 승격 연결, 만료 스케줄러 복구)은 **제외**하고 후속 과제로만 기록.

**API 호환성 원칙**: URL 경로(`/api/tosspay/**`)와 응답 JSON 필드는 유지. 클래스/내부 구조만 변경.

## 목표 구조

```
paymentsapi/
├── api/PaymentController.java              (WidgetController에서 rename, URL 불변)
├── client/
│   ├── TossPaymentsClient.java             (신규 — 모든 Toss HTTP 호출 캡슐화)
│   └── dto/
│       ├── TossPaymentApiResponse.java     (dto/request/TosspaymentRequestDTO에서 이동+rename — 실제로는 Toss 응답 매핑)
│       └── TossErrorResponse.java          (신규 — Toss 오류 본문 {code, message})
├── service/
│   ├── PaymentService.java                 (TossService에서 rename, 클래스 레벨 @Transactional 제거)
│   └── SubscriptionManagementService.java  (신규 — @Transactional 영속화 로직. ※ util/SubscriptionService와 빈 이름 충돌 방지 위해 이 이름 사용)
├── entity/   (+ TossPaymentStatus enum 신규)
├── repository/ (파일 유지, 메서드 정리)
└── dto/
    ├── request/  (PaymentConfirmReqDTO, PaymentInfoRequestDTO — SubManagementReqDTO 삭제)
    └── response/ (TossPaymentResponseDTO, SubscriptionResponseDTO 신규 — OrderInfoResponseDTO 삭제)
```

## 구현 단계 (각 단계에서 컴파일 유지)

### 1단계 — 예외 추가 (독립)
`exception/` 패키지에 신규: `InvalidPaymentAmountException`(400), `PaymentAccessDeniedException`(403), `TossApiException`(502, Toss의 code/message 보유).
`GlobalExceptionHandler`에 위 3개 + 누락된 핸들러 추가: `OrderNotFoundException`→404, `SubscriptionNotFoundException`→404, `CustomUnauthorizedException`→401.

### 2단계 — 엔티티 수정 (독립)
- **`TossPaymentStatus` enum 신규**: `READY, IN_PROGRESS, WAITING_FOR_DEPOSIT, DONE, CANCELED, PARTIAL_CANCELED, ABORTED, EXPIRED` (Toss 공식 상태 전체). `PaidInfo.paymentStatus`를 `String` → 이 enum(`@Enumerated(STRING)`)으로 변경. DB에는 이미 Toss 상태 문자열("DONE" 등)이 그대로 저장돼 있으므로 데이터 호환 — 기존 도메인 enum `PaymentStatus`(PENDING/COMPLETED/CANCELED)는 값이 달라 사용 불가, `PaymentInfo`에서 그대로 유지.
- `PaidInfo`: 취소용 명명 변경 메서드 `applyCancellation(TossPaymentStatus)` 추가. `@EqualsAndHashCode`를 id 기반으로 제한(다른 payment 엔티티도 동일 처리).
- `PaymentInfo`: 필드 `create_at` → `createdAt` (`@Column(name = "create_at")`으로 컬럼명 보존), `LocalDateTime.now()` 기본값 대신 `@CreationTimestamp`.
- `SubManagement`: `static SubManagement of(Subscription, PaidInfo)` 팩토리 추가.

### 3단계 — DTO 정리 (2단계 이후)
- `TosspaymentRequestDTO` → `client/dto/TossPaymentApiResponse`로 이동+rename.
  - 3개의 중복 `toEntity` 오버로드를 단일 `toEntity(User)`로 통합 (virtualAccount null 여부로 분기 — 기존 3개 중 2개는 사실상 동일).
  - `paymentStatus`를 `TossPaymentStatus` 타입으로 (Jackson이 `"DONE"` → enum 매핑).
  - `requestedAt` 변환(ISO offset 문자열 → LocalDateTime)을 명시적 `@JsonProperty` setter로 — 현재 private setter는 Jackson 기본 가시성(ANY) 덕에 우연히 동작 중이므로 의도를 드러나게 수정. 오해 소지 있는 `@JsonFormat` 제거.
- `TossPaymentResponseDTO`: 필드 `OrderId` → `orderId` (Lombok getter 때문에 JSON은 이미 `orderId` — 계약 변화 없음).
- 신규 `SubscriptionResponseDTO` (`subId, period, price, orderName, description` — 엔티티와 동일 필드명으로 JSON 불변) + `static from(Subscription)`.
- 삭제: `OrderInfoResponseDTO`(빈 클래스), `SubManagementReqDTO`(`SubManagement.of`로 대체).
- `PaymentInfoRequestDTO`: `toEntity(Long payId)` 단일화.

### 4단계 — TossPaymentsClient 추출 (1,3단계 이후)
`WebClient.Builder` 주입으로 생성자에서 WebClient 1개 구성 (매 호출 `new RestTemplate()`/`WebClient.create()` 제거). Basic auth 헤더는 생성자에서 1회 생성(`TossPaymentsConfig.getTossSecretKey()`).
- `confirm(paymentKey, orderId, amount)` — POST /v1/payments/confirm
- `cancel(paymentKey, reason)` — POST /v1/payments/{paymentKey}/cancel. **버그 수정: `params.toString()` → `.bodyValue(Map.of("cancelReason", reason))` + `contentType(APPLICATION_JSON)`** (기존 코드는 유효하지 않은 JSON 전송, TossService.java:190)
- `getOrderByOrderId(orderId)` — GET /v1/payments/orders/{orderId}
- 공통 오류 처리: `.onStatus(isError, ...)` → Toss 오류 본문을 `TossErrorResponse`로 파싱해 `TossApiException` (기존: 미처리 WebClientResponseException → 500).
- URL은 하드코딩 문자열 대신 클라이언트 내 상수로 통합.
- **미사용+버그 메서드 `TossService.getOrderInfo()` 삭제** (컨트롤러는 `getOrderInfoByOrderId`만 호출, orderKey를 {paymentKey} 슬롯에 넣는 버그도 있음).

### 5단계 — Repository 정리 (독립)
- `PaidInfoRepository.updatePaymentStatus` **삭제** (SQL 문법 자체가 깨져 있고 존재하지 않는 컬럼 `paid_at` 참조, 호출처 없음) + 잘못된 `javax.lang.model.element.Name` import 제거.
- `PaymentInfoRepository.findPaymentByOrderId` 삭제 (nullable 반환, `findByOrderId` Optional과 중복).
- `SubManagementRepository.findByUserIdWithFetchJoin`: `Optional<List<...>>` → `List<...>`.

### 6단계 — 서비스 재구성 (1~5단계 이후)
- `TossService` → `PaymentService` rename, 클래스 레벨 `@Transactional` 제거. 오케스트레이션만: 검증 → Toss 호출(트랜잭션 밖) → `SubscriptionManagementService`에 영속화 위임.
- `confirm()`: `findByOrderId(...).orElseThrow(OrderNotFoundException)` (NPE 수정, TossService.java:77-79), 금액 불일치 시 `InvalidPaymentAmountException` (orderId 동등 비교는 orderId로 조회했으므로 항진식 — 제거).
- **`SubscriptionManagementService`** (신규 빈 — 별도 빈이라 self-invocation 프록시 문제도 회피):
  - `@Transactional completePayment(User, TossPaymentApiResponse)`: PaidInfo 저장 → status가 `DONE`이면(enum 비교, 기존 `"가상계좌"` 문자열 분기 대신) SubManagement 생성 + SubscriptionInfo upsert + 만료일 계산. 원자적 커밋/롤백.
  - `@Transactional applyCancellation(paidId, ...)`: 조회 후 `paidInfo.applyCancellation(...)` — dirty checking으로 UPDATE. **버그 수정: 기존 `toEntity(user, payId)`가 payId를 무시해 취소 시 paid_info 중복 행 INSERT되던 문제 해결** (TossService.java:195).
  - 만료일 계산: 빈 리스트면 NPE 대신 `SubscriptionNotFoundException` (TossService.java:222 `Objects.requireNonNull` NPE 수정). 계산 로직(최초 결제일 + 기간 합산)은 동작 보존.
- `cancel()`에 **소유권 검증 추가**: `paidInfo.getUser().getId().equals(user.getId())` 아니면 `PaymentAccessDeniedException` (현재 타인 주문 취소 가능). 취소 사유 "단순 변심"은 명명 상수로 유지.
- `findUserByToken`의 raw `RuntimeException` → 기존 `CustomUnauthorizedException`.
- `throws JsonProcessingException` 제거 (실제로 던지는 코드 없음).

### 7단계 — 컨트롤러 (3,6단계 이후)
- `WidgetController` → `PaymentController` rename, `@RequestMapping("/api/tosspay")` 불변.
- `GET /subscription`: 엔티티 직접 반환 → `List<SubscriptionResponseDTO>`.
- 삭제: 수동 `Logger`(line 29, @Slf4j와 중복), `confirmPayment`의 불필요 `@ResponseBody`와 try/catch, `tossCancel`의 `catch(Exception) → RuntimeException` 래핑, `tossOrder`의 무의미한 try/catch(호출이 try 밖에 있음).
- **view 반환 엔드포인트 `GET /success`, `/`, `/fail` 삭제** — @RestController에서 문자열 리터럴 반환(뷰 리졸빙 안 됨), 템플릿 엔진 없음, 인증 게이트에 걸림 → 사실상 죽은 코드. (리스크 항목 참조)

### 8단계 — 테스트 (JUnit 5 + Mockito + AssertJ, Spring 컨텍스트 없이)
※ `application.yml`이 gitignore로 리포에 없어 `@SpringBootTest`는 로컬에서 실패 — 순수 단위 테스트로 작성.
- `client/TossPaymentsClientTest`: `WebClient.builder().exchangeFunction(stub)`으로 HTTP 스텁.
  - **cancel 본문이 유효한 JSON `{"cancelReason":"단순 변심"}`인지** (회귀 테스트), confirm의 본문/Basic auth 헤더, `requestedAt` ISO 문자열 → LocalDateTime 변환, `"DONE"` → enum 매핑, Toss 4xx 오류 본문 → `TossApiException`.
- `service/PaymentServiceTest` (`@ExtendWith(MockitoExtension)`): confirm 성공 / 금액 불일치(클라이언트 미호출 검증) / 주문 없음 / Toss 오류 전파 / cancel 성공 / 비소유자 403 / 인증정보 없음 401.
- `service/SubscriptionManagementServiceTest`: DONE → 구독 행 생성, `WAITING_FOR_DEPOSIT`(가상계좌) → PaidInfo만 저장, 미등록 orderName → 예외, 만료일 계산(복수 구독 합산 / 빈 리스트 가드).

## 검증

- 각 단계 후: `./gradlew compileJava compileTestJava`
- 테스트: `./gradlew test --tests 'com.example.HonBam.paymentsapi.*'` — 전체 `test`는 기존 Chat/컨텍스트 테스트가 application.yml·Docker 의존으로 이 체크아웃에서 원래 실패하므로 선택 실행. 시작 전 기준 상태 기록.
- Toss 실호출 E2E는 운영자 테스트 키 + 로컬 application.yml 필요 — 범위 밖. cancel 본문/오류 매핑 단위 테스트가 리포 내 대리 검증.

## 리스크 / 프론트엔드 공지 사항

1. `/success`, `/`, `/fail` 삭제 — 운영 application.yml의 `payment.toss.success_url/fail_url`이 이 API 경로를 가리키지 않는지 머지 전 확인.
2. `TossPaymentStatus` enum — 배포 전 실 DB에서 `SELECT DISTINCT payment_status FROM paid_info`로 목록 외 값 없는지 확인.
3. 오류 응답 계약 변화: 결제 실패가 raw 문자열 400/500 → `ErrorResponse{errorCode,message}` JSON + 정확한 상태코드(401/403/404/502).
4. 취소가 중복 행 INSERT 대신 in-place UPDATE — 과거 생성된 중복 행은 그대로 남음(무해).

## 후속 과제 (이번 범위 밖, PR 설명에 기록)

- 가상계좌 입금 webhook 수신 (현재 가상계좌 결제가 DONE으로 갱신될 방법이 없음)
- 결제 성공 ↔ User PREMIUM 역할/구독상태 연결 (현재 완전히 분리됨)
- `util/SubscriptionService` 만료 스케줄러 복구 (로직 주석 처리 + 하드코딩 날짜)
- 만료일이 "최초 결제일 기준"으로 계산되는 비즈니스 로직 재검토
- confirm 시점 소유권 검증 (`PaymentInfo`에 user FK 추가 필요 — 스키마 변경이라 보류)
