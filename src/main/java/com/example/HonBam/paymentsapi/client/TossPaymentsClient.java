package com.example.HonBam.paymentsapi.client;

import com.example.HonBam.config.TossPaymentsConfig;
import com.example.HonBam.exception.TossApiException;
import com.example.HonBam.paymentsapi.client.dto.TossErrorResponse;
import com.example.HonBam.paymentsapi.client.dto.TossPaymentApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

// Toss Payments API 호출 전담 (HTTP 통신만 담당, 영속화 로직 없음)
@Component
@Slf4j
public class TossPaymentsClient {

    private static final String BASE_URL = "https://api.tosspayments.com";
    private static final String CONFIRM_URI = "/v1/payments/confirm";
    private static final String CANCEL_URI = "/v1/payments/{paymentKey}/cancel";
    private static final String ORDER_URI = "/v1/payments/orders/{orderId}";

    private final WebClient webClient;

    public TossPaymentsClient(WebClient.Builder webClientBuilder, TossPaymentsConfig tossPaymentsConfig) {
        String basicAuth = "Basic " + Base64.getEncoder().encodeToString(
                (tossPaymentsConfig.getTossSecretKey() + ":").getBytes(StandardCharsets.UTF_8));
        this.webClient = webClientBuilder
                .baseUrl(BASE_URL)
                .defaultHeader(HttpHeaders.AUTHORIZATION, basicAuth)
                .build();
    }

    // 결제 승인: POST /v1/payments/confirm
    public TossPaymentApiResponse confirm(String paymentKey, String orderId, int amount) {
        return webClient.post()
                .uri(CONFIRM_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "paymentKey", paymentKey,
                        "orderId", orderId,
                        "amount", amount))
                .retrieve()
                .onStatus(HttpStatus::isError, this::toTossApiException)
                .bodyToMono(TossPaymentApiResponse.class)
                .block();
    }

    // 결제 취소: POST /v1/payments/{paymentKey}/cancel
    public TossPaymentApiResponse cancel(String paymentKey, String cancelReason) {
        return webClient.post()
                .uri(CANCEL_URI, paymentKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("cancelReason", cancelReason))
                .retrieve()
                .onStatus(HttpStatus::isError, this::toTossApiException)
                .bodyToMono(TossPaymentApiResponse.class)
                .block();
    }

    // 주문번호로 결제 조회: GET /v1/payments/orders/{orderId}
    public TossPaymentApiResponse getOrderByOrderId(String orderId) {
        return webClient.get()
                .uri(ORDER_URI, orderId)
                .retrieve()
                .onStatus(HttpStatus::isError, this::toTossApiException)
                .bodyToMono(TossPaymentApiResponse.class)
                .block();
    }

    private Mono<TossApiException> toTossApiException(ClientResponse response) {
        return response.bodyToMono(TossErrorResponse.class)
                .defaultIfEmpty(new TossErrorResponse("UNKNOWN", "토스 API 호출에 실패했습니다."))
                .map(error -> {
                    log.error("토스 API 오류 응답: status={}, code={}, message={}",
                            response.statusCode(), error.getCode(), error.getMessage());
                    return new TossApiException(error.getCode(), error.getMessage());
                });
    }

}
