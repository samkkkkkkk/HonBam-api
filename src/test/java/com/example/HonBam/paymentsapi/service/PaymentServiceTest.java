package com.example.HonBam.paymentsapi.service;

import com.example.HonBam.auth.TokenUserInfo;
import com.example.HonBam.exception.CustomUnauthorizedException;
import com.example.HonBam.exception.InvalidPaymentAmountException;
import com.example.HonBam.exception.OrderNotFoundException;
import com.example.HonBam.exception.PaymentAccessDeniedException;
import com.example.HonBam.exception.TossApiException;
import com.example.HonBam.paymentsapi.client.TossPaymentsClient;
import com.example.HonBam.paymentsapi.client.dto.TossPaymentApiResponse;
import com.example.HonBam.paymentsapi.dto.request.PaymentConfirmReqDTO;
import com.example.HonBam.paymentsapi.dto.response.TossPaymentResponseDTO;
import com.example.HonBam.paymentsapi.entity.PaidInfo;
import com.example.HonBam.paymentsapi.entity.PaymentInfo;
import com.example.HonBam.paymentsapi.entity.TossPaymentStatus;
import com.example.HonBam.paymentsapi.repository.PaidInfoRepository;
import com.example.HonBam.paymentsapi.repository.PaymentInfoRepository;
import com.example.HonBam.paymentsapi.repository.SubscriptionRepository;
import com.example.HonBam.userapi.entity.User;
import com.example.HonBam.userapi.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock UserRepository userRepository;
    @Mock PaymentInfoRepository paymentInfoRepository;
    @Mock PaidInfoRepository paidInfoRepository;
    @Mock SubscriptionRepository subscriptionRepository;
    @Mock TossPaymentsClient tossPaymentsClient;
    @Mock SubscriptionManagementService subscriptionManagementService;

    @InjectMocks PaymentService paymentService;

    private final TokenUserInfo userInfo = TokenUserInfo.builder().userId("user-1").build();
    private final User user = User.builder().id("user-1").build();
    private final User otherUser = User.builder().id("user-2").build();
    private final PaymentConfirmReqDTO reqDTO =
            new PaymentConfirmReqDTO("order-1", 10000, "pk_123", "카드");

    private PaidInfo paidInfo(User owner, TossPaymentStatus status) {
        return PaidInfo.builder()
                .paidId(5L)
                .orderId("order-1")
                .orderName("1개월 구독")
                .amount(10000)
                .method("카드")
                .requestedAt(LocalDateTime.of(2024, 1, 1, 10, 0))
                .paymentStatus(status)
                .user(owner)
                .build();
    }

    @Test
    @DisplayName("confirm 성공: 검증 → 토스 승인 → 영속화 위임 → 응답 DTO 반환")
    void confirmSuccess() {
        given(userRepository.findById("user-1")).willReturn(Optional.of(user));
        given(paymentInfoRepository.findByOrderId("order-1"))
                .willReturn(Optional.of(PaymentInfo.builder().orderId("order-1").amount(10000).build()));
        TossPaymentApiResponse apiResponse = TossPaymentApiResponse.builder()
                .orderId("order-1").amount(10000).paymentStatus(TossPaymentStatus.DONE).build();
        given(tossPaymentsClient.confirm("pk_123", "order-1", 10000)).willReturn(apiResponse);
        given(subscriptionManagementService.completePayment(user, apiResponse))
                .willReturn(paidInfo(user, TossPaymentStatus.DONE));

        TossPaymentResponseDTO result = paymentService.confirm(reqDTO, userInfo);

        assertThat(result.getOrderId()).isEqualTo("order-1");
        assertThat(result.getOrderName()).isEqualTo("1개월 구독");
        assertThat(result.getAmount()).isEqualTo(10000);
        then(subscriptionManagementService).should().completePayment(user, apiResponse);
    }

    @Test
    @DisplayName("confirm: 금액 불일치면 토스 호출 없이 InvalidPaymentAmountException")
    void confirmRejectsAmountMismatchWithoutCallingToss() {
        given(userRepository.findById("user-1")).willReturn(Optional.of(user));
        given(paymentInfoRepository.findByOrderId("order-1"))
                .willReturn(Optional.of(PaymentInfo.builder().orderId("order-1").amount(99999).build()));

        assertThatThrownBy(() -> paymentService.confirm(reqDTO, userInfo))
                .isInstanceOf(InvalidPaymentAmountException.class);

        then(tossPaymentsClient).should(never()).confirm(anyString(), anyString(), anyInt());
        then(subscriptionManagementService).should(never()).completePayment(any(), any());
    }

    @Test
    @DisplayName("confirm: 주문이 없으면 OrderNotFoundException")
    void confirmRejectsUnknownOrder() {
        given(userRepository.findById("user-1")).willReturn(Optional.of(user));
        given(paymentInfoRepository.findByOrderId("order-1")).willReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.confirm(reqDTO, userInfo))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    @DisplayName("confirm: 토스 API 오류(TossApiException)는 그대로 전파된다")
    void confirmPropagatesTossApiException() {
        given(userRepository.findById("user-1")).willReturn(Optional.of(user));
        given(paymentInfoRepository.findByOrderId("order-1"))
                .willReturn(Optional.of(PaymentInfo.builder().orderId("order-1").amount(10000).build()));
        given(tossPaymentsClient.confirm("pk_123", "order-1", 10000))
                .willThrow(new TossApiException("REJECT_CARD_COMPANY", "카드사 거절"));

        assertThatThrownBy(() -> paymentService.confirm(reqDTO, userInfo))
                .isInstanceOf(TossApiException.class);

        then(subscriptionManagementService).should(never()).completePayment(any(), any());
    }

    @Test
    @DisplayName("cancel 성공: 소유자 확인 → 토스 취소('단순 변심') → 상태 갱신 위임")
    void cancelSuccess() {
        given(userRepository.findById("user-1")).willReturn(Optional.of(user));
        given(paidInfoRepository.findByOrderId("order-1"))
                .willReturn(Optional.of(paidInfo(user, TossPaymentStatus.DONE)));
        TossPaymentApiResponse cancelResponse = TossPaymentApiResponse.builder()
                .orderId("order-1").paymentStatus(TossPaymentStatus.CANCELED).build();
        given(tossPaymentsClient.cancel("pk_123", "단순 변심")).willReturn(cancelResponse);
        given(subscriptionManagementService.applyCancellation(5L, TossPaymentStatus.CANCELED))
                .willReturn(paidInfo(user, TossPaymentStatus.CANCELED));

        TossPaymentResponseDTO result = paymentService.cancel(userInfo, reqDTO);

        assertThat(result.getOrderId()).isEqualTo("order-1");
        then(tossPaymentsClient).should().cancel("pk_123", "단순 변심");
        then(subscriptionManagementService).should().applyCancellation(5L, TossPaymentStatus.CANCELED);
    }

    @Test
    @DisplayName("cancel: 타인의 주문이면 토스 호출 없이 PaymentAccessDeniedException")
    void cancelRejectsNonOwner() {
        given(userRepository.findById("user-1")).willReturn(Optional.of(user));
        given(paidInfoRepository.findByOrderId("order-1"))
                .willReturn(Optional.of(paidInfo(otherUser, TossPaymentStatus.DONE)));

        assertThatThrownBy(() -> paymentService.cancel(userInfo, reqDTO))
                .isInstanceOf(PaymentAccessDeniedException.class);

        then(tossPaymentsClient).should(never()).cancel(anyString(), anyString());
        then(subscriptionManagementService).should(never()).applyCancellation(any(), any());
    }

    @Test
    @DisplayName("인증 정보가 없으면 CustomUnauthorizedException")
    void rejectsMissingAuthentication() {
        assertThatThrownBy(() -> paymentService.confirm(reqDTO, null))
                .isInstanceOf(CustomUnauthorizedException.class);

        assertThatThrownBy(() -> paymentService.cancel(TokenUserInfo.builder().build(), reqDTO))
                .isInstanceOf(CustomUnauthorizedException.class);
    }

}
