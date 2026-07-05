package com.example.HonBam.paymentsapi.client;

import com.example.HonBam.config.TossPaymentsConfig;
import com.example.HonBam.exception.TossApiException;
import com.example.HonBam.paymentsapi.client.dto.TossPaymentApiResponse;
import com.example.HonBam.paymentsapi.entity.TossPaymentStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.reactive.MockClientHttpRequest;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

// Spring 컨텍스트 없이 ExchangeFunction 스텁으로 실제 직렬화/역직렬화까지 검증
class TossPaymentsClientTest {

    private static final String SECRET_KEY = "test_secret_key";
    private static final String CONFIRM_RESPONSE_JSON = "{"
            + "\"paymentKey\":\"pk_123\","
            + "\"orderId\":\"order-1\","
            + "\"totalAmount\":10000,"
            + "\"status\":\"DONE\","
            + "\"orderName\":\"1개월 구독\","
            + "\"method\":\"카드\","
            + "\"requestedAt\":\"2024-01-01T10:00:00+09:00\""
            + "}";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ClientRequest capturedRequest;
    private String capturedBody;

    private TossPaymentsClient clientReturning(HttpStatus status, String responseJson) {
        TossPaymentsConfig config = mock(TossPaymentsConfig.class);
        given(config.getTossSecretKey()).willReturn(SECRET_KEY);

        ExchangeFunction stub = request -> {
            capturedRequest = request;
            MockClientHttpRequest mockRequest = new MockClientHttpRequest(request.method(), request.url());
            request.writeTo(mockRequest, ExchangeStrategies.withDefaults()).block();
            capturedBody = mockRequest.getBodyAsString().block();
            return Mono.just(ClientResponse.create(status)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(responseJson)
                    .build());
        };
        return new TossPaymentsClient(WebClient.builder().exchangeFunction(stub), config);
    }

    @Test
    @DisplayName("cancel은 유효한 JSON 본문 {\"cancelReason\":...}을 전송한다 (회귀 테스트)")
    void cancelSendsValidJsonBody() throws Exception {
        TossPaymentsClient client = clientReturning(HttpStatus.OK, CONFIRM_RESPONSE_JSON);

        client.cancel("pk_123", "단순 변심");

        JsonNode body = objectMapper.readTree(capturedBody);
        assertThat(body.get("cancelReason").asText()).isEqualTo("단순 변심");
        assertThat(capturedRequest.method()).isEqualTo(HttpMethod.POST);
        assertThat(capturedRequest.url().getPath()).isEqualTo("/v1/payments/pk_123/cancel");
        assertThat(capturedRequest.headers().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
    }

    @Test
    @DisplayName("confirm은 paymentKey/orderId/amount 본문과 Basic 인증 헤더를 전송한다")
    void confirmSendsBodyAndBasicAuthHeader() throws Exception {
        TossPaymentsClient client = clientReturning(HttpStatus.OK, CONFIRM_RESPONSE_JSON);

        client.confirm("pk_123", "order-1", 10000);

        JsonNode body = objectMapper.readTree(capturedBody);
        assertThat(body.get("paymentKey").asText()).isEqualTo("pk_123");
        assertThat(body.get("orderId").asText()).isEqualTo("order-1");
        assertThat(body.get("amount").asInt()).isEqualTo(10000);

        String expectedAuth = "Basic " + Base64.getEncoder()
                .encodeToString((SECRET_KEY + ":").getBytes(StandardCharsets.UTF_8));
        assertThat(capturedRequest.headers().getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo(expectedAuth);
        assertThat(capturedRequest.url().getPath()).isEqualTo("/v1/payments/confirm");
    }

    @Test
    @DisplayName("응답의 requestedAt(ISO offset 문자열)을 LocalDateTime으로 변환한다")
    void mapsRequestedAtToLocalDateTime() {
        TossPaymentsClient client = clientReturning(HttpStatus.OK, CONFIRM_RESPONSE_JSON);

        TossPaymentApiResponse response = client.confirm("pk_123", "order-1", 10000);

        assertThat(response.getRequestedAt()).isEqualTo(LocalDateTime.of(2024, 1, 1, 10, 0, 0));
    }

    @Test
    @DisplayName("응답의 status 문자열을 TossPaymentStatus enum으로 매핑한다")
    void mapsStatusStringToEnum() {
        TossPaymentsClient client = clientReturning(HttpStatus.OK, CONFIRM_RESPONSE_JSON);

        TossPaymentApiResponse response = client.confirm("pk_123", "order-1", 10000);

        assertThat(response.getPaymentStatus()).isEqualTo(TossPaymentStatus.DONE);
        assertThat(response.getAmount()).isEqualTo(10000);
        assertThat(response.getOrderId()).isEqualTo("order-1");
    }

    @Test
    @DisplayName("토스 4xx 오류 본문을 TossApiException(code, message)으로 변환한다")
    void mapsTossErrorBodyToTossApiException() {
        TossPaymentsClient client = clientReturning(HttpStatus.BAD_REQUEST,
                "{\"code\":\"NOT_FOUND_PAYMENT\",\"message\":\"존재하지 않는 결제 입니다.\"}");

        assertThatThrownBy(() -> client.confirm("pk_bad", "order-1", 10000))
                .isInstanceOf(TossApiException.class)
                .hasMessage("존재하지 않는 결제 입니다.")
                .extracting(e -> ((TossApiException) e).getCode())
                .isEqualTo("NOT_FOUND_PAYMENT");
    }

}
