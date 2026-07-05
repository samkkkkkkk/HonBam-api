# Payment 리팩토링 작업 체크리스트

> 원본 계획: [payment-refactoring-plan.md](./payment-refactoring-plan.md)
> 각 단계 완료 시 체크박스에 `x`를 표시하고, 단계 끝의 컴파일 검증까지 통과한 뒤 다음 단계로 진행한다.

## 1단계 — 예외 추가 (독립)

- [x] `InvalidPaymentAmountException` 생성 (400)
- [x] `PaymentAccessDeniedException` 생성 (403)
- [x] `TossApiException` 생성 (502, Toss의 code/message 보유)
- [x] `GlobalExceptionHandler`에 `InvalidPaymentAmountException` → 400 핸들러 추가
- [x] `GlobalExceptionHandler`에 `PaymentAccessDeniedException` → 403 핸들러 추가
- [x] `GlobalExceptionHandler`에 `TossApiException` → 502 핸들러 추가
- [x] `GlobalExceptionHandler`에 누락 핸들러 추가: `OrderNotFoundException` → 404
- [x] `GlobalExceptionHandler`에 누락 핸들러 추가: `SubscriptionNotFoundException` → 404
- [x] `GlobalExceptionHandler`에 누락 핸들러 추가: `CustomUnauthorizedException` → 401
- [x] `./gradlew compileJava compileTestJava` 통과

## 2단계 — 엔티티 수정 (독립)

- [x] `TossPaymentStatus` enum 신규 생성: `READY, IN_PROGRESS, WAITING_FOR_DEPOSIT, DONE, CANCELED, PARTIAL_CANCELED, ABORTED, EXPIRED`
- [x] `PaidInfo.paymentStatus`: `String` → `TossPaymentStatus` (`@Enumerated(STRING)`)
- [x] `PaidInfo`에 취소용 메서드 `applyCancellation(TossPaymentStatus)` 추가
- [x] payment 엔티티들의 `@EqualsAndHashCode`를 id 기반으로 제한
- [x] `PaymentInfo`: 필드 `create_at` → `createdAt` (`@Column(name = "create_at")`으로 컬럼명 보존)
- [x] `PaymentInfo`: `LocalDateTime.now()` 기본값 → `@CreationTimestamp`
- [x] `SubManagement`에 `static SubManagement of(Subscription, PaidInfo)` 팩토리 추가
- [x] `./gradlew compileJava compileTestJava` 통과

## 3단계 — DTO 정리 (2단계 이후)

- [x] `TosspaymentRequestDTO` → `client/dto/TossPaymentApiResponse`로 이동+rename
- [x] 3개의 중복 `toEntity` 오버로드를 단일 `toEntity(User)`로 통합 (virtualAccount null 여부로 분기)
- [x] `paymentStatus`를 `TossPaymentStatus` 타입으로 변경 (Jackson `"DONE"` → enum 매핑)
- [x] `requestedAt` 변환(ISO offset 문자열 → LocalDateTime)을 명시적 `@JsonProperty` setter로 변경, 오해 소지 있는 `@JsonFormat` 제거
- [x] `TossPaymentResponseDTO`: 필드 `OrderId` → `orderId` (JSON 계약 변화 없음)
- [x] `SubscriptionResponseDTO` 신규 생성 (`subId, period, price, orderName, description` + `static from(Subscription)`)
- [x] `OrderInfoResponseDTO` 삭제 (빈 클래스)
- [x] `SubManagementReqDTO` 삭제 (`SubManagement.of`로 대체)
- [x] `PaymentInfoRequestDTO`: `toEntity(Long payId)` 단일화
- [x] `./gradlew compileJava compileTestJava` 통과

## 4단계 — TossPaymentsClient 추출 (1, 3단계 이후)

- [x] `client/TossPaymentsClient` 신규: `WebClient.Builder` 주입, 생성자에서 WebClient 1개 구성 (매 호출 `new RestTemplate()`/`WebClient.create()` 제거)
- [x] Basic auth 헤더를 생성자에서 1회 생성 (`TossPaymentsConfig.getTossSecretKey()`)
- [x] `confirm(paymentKey, orderId, amount)` — POST `/v1/payments/confirm`
- [x] `cancel(paymentKey, reason)` — POST `/v1/payments/{paymentKey}/cancel` — **버그 수정**: `params.toString()` → `.bodyValue(Map.of("cancelReason", reason))` + `contentType(APPLICATION_JSON)` (TossService.java:190)
- [x] `getOrderByOrderId(orderId)` — GET `/v1/payments/orders/{orderId}`
- [x] `TossErrorResponse` 신규 + 공통 오류 처리: `.onStatus(isError, ...)` → Toss 오류 본문 파싱해 `TossApiException`
- [x] URL 하드코딩 문자열 → 클라이언트 내 상수로 통합
- [x] 미사용+버그 메서드 `TossService.getOrderInfo()` 삭제
- [x] `./gradlew compileJava compileTestJava` 통과

## 5단계 — Repository 정리 (독립)

- [x] `PaidInfoRepository.updatePaymentStatus` 삭제 (깨진 SQL, 존재하지 않는 컬럼 `paid_at`, 호출처 없음) + 잘못된 `javax.lang.model.element.Name` import 제거
- [x] `PaymentInfoRepository.findPaymentByOrderId` 삭제 (`findByOrderId` Optional과 중복)
- [x] `SubManagementRepository.findByUserIdWithFetchJoin`: `Optional<List<...>>` → `List<...>`
- [x] `./gradlew compileJava compileTestJava` 통과

## 6단계 — 서비스 재구성 (1~5단계 이후)

- [x] `TossService` → `PaymentService` rename, 클래스 레벨 `@Transactional` 제거 (오케스트레이션만: 검증 → Toss 호출(트랜잭션 밖) → 영속화 위임)
- [x] `confirm()`: `findByOrderId(...).orElseThrow(OrderNotFoundException)` (NPE 수정, TossService.java:77-79) — 5단계에서 선반영
- [x] `confirm()`: 금액 불일치 시 `InvalidPaymentAmountException`, 항진식 orderId 동등 비교 제거
- [x] `SubscriptionManagementService` 신규 빈 생성 (self-invocation 프록시 문제 회피)
- [x] `@Transactional completePayment(User, TossPaymentApiResponse)`: PaidInfo 저장 → status `DONE`이면(enum 비교) SubManagement 생성 + SubscriptionInfo upsert + 만료일 계산 (기존 `"가상계좌"` 문자열 분기 제거)
- [x] `@Transactional applyCancellation(paidId, ...)`: dirty checking UPDATE — **버그 수정**: 취소 시 paid_info 중복 행 INSERT 문제 해결 (TossService.java:195)
- [x] 만료일 계산: 빈 리스트면 `SubscriptionNotFoundException` (TossService.java:222 NPE 수정), 계산 로직(최초 결제일 + 기간 합산)은 동작 보존
- [x] `cancel()`에 소유권 검증 추가: 비소유자면 `PaymentAccessDeniedException`, 취소 사유 "단순 변심"은 명명 상수로 유지
- [x] `findUserByToken`의 raw `RuntimeException` → `CustomUnauthorizedException`
- [x] `throws JsonProcessingException` 제거
- [x] `./gradlew compileJava compileTestJava` 통과

## 7단계 — 컨트롤러 (3, 6단계 이후)

- [x] `WidgetController` → `PaymentController` rename, `@RequestMapping("/api/tosspay")` 불변
- [x] `GET /subscription`: 엔티티 직접 반환 → `List<SubscriptionResponseDTO>`
- [x] 수동 `Logger` 삭제 (line 29, `@Slf4j`와 중복)
- [x] `confirmPayment`의 불필요 `@ResponseBody`와 try/catch 삭제 (try/catch는 6단계에서 선반영)
- [x] `tossCancel`의 `catch(Exception) → RuntimeException` 래핑 삭제 (6단계에서 선반영)
- [x] `tossOrder`의 무의미한 try/catch 삭제
- [x] view 반환 엔드포인트 `GET /success`, `/`, `/fail` 삭제 (죽은 코드)
- [x] `./gradlew compileJava compileTestJava` 통과

## 8단계 — 테스트 (JUnit 5 + Mockito + AssertJ, Spring 컨텍스트 없이)

### `client/TossPaymentsClientTest` (`WebClient.builder().exchangeFunction(stub)` HTTP 스텁)

- [x] cancel 본문이 유효한 JSON `{"cancelReason":"단순 변심"}`인지 (회귀 테스트)
- [x] confirm의 본문/Basic auth 헤더 검증
- [x] `requestedAt` ISO 문자열 → LocalDateTime 변환
- [x] `"DONE"` → enum 매핑
- [x] Toss 4xx 오류 본문 → `TossApiException`

### `service/PaymentServiceTest` (`@ExtendWith(MockitoExtension)`)

- [x] confirm 성공
- [x] 금액 불일치 (클라이언트 미호출 검증)
- [x] 주문 없음
- [x] Toss 오류 전파
- [x] cancel 성공
- [x] 비소유자 403
- [x] 인증정보 없음 401

### `service/SubscriptionManagementServiceTest`

- [x] `DONE` → 구독 행 생성
- [x] `WAITING_FOR_DEPOSIT`(가상계좌) → PaidInfo만 저장
- [x] 미등록 orderName → 예외
- [x] `applyCancellation` → 기존 행 상태만 변경 (INSERT 없음)
- [x] 만료일 계산: 복수 구독 합산
- [x] 만료일 계산: 빈 리스트 가드

### 실행

- [x] `./gradlew test --tests 'com.example.HonBam.paymentsapi.*'` 통과 — 18개 테스트 전부 성공 (전체 `test`는 기존 Chat/컨텍스트 테스트가 application.yml·Docker 의존으로 원래 실패 — 선택 실행)

## 머지 전 확인 (리스크)

- [ ] 운영 application.yml의 `payment.toss.success_url/fail_url`이 삭제된 `/success`, `/`, `/fail` 경로를 가리키지 않는지 확인
- [ ] 실 DB에서 `SELECT DISTINCT payment_status FROM paid_info` — `TossPaymentStatus` enum 목록 외 값 없는지 확인
- [ ] 오류 응답 계약 변화 프론트엔드 공지: raw 문자열 400/500 → `ErrorResponse{errorCode,message}` JSON + 정확한 상태코드(401/403/404/502)
- [ ] 취소 동작 변화 공지: 중복 행 INSERT → in-place UPDATE (과거 중복 행은 그대로 남음, 무해)

## 후속 과제 (이번 범위 밖 — PR 설명에 기록)

- 가상계좌 입금 webhook 수신 (현재 가상계좌 결제가 DONE으로 갱신될 방법이 없음)
- 결제 성공 ↔ User PREMIUM 역할/구독상태 연결
- `util/SubscriptionService` 만료 스케줄러 복구
- 만료일 "최초 결제일 기준" 계산 비즈니스 로직 재검토
- confirm 시점 소유권 검증 (`PaymentInfo`에 user FK 추가 필요 — 스키마 변경이라 보류)
