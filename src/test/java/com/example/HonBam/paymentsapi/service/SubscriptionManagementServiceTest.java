package com.example.HonBam.paymentsapi.service;

import com.example.HonBam.exception.SubscriptionNotFoundException;
import com.example.HonBam.paymentsapi.client.dto.TossPaymentApiResponse;
import com.example.HonBam.paymentsapi.entity.PaidInfo;
import com.example.HonBam.paymentsapi.entity.SubManagement;
import com.example.HonBam.paymentsapi.entity.Subscription;
import com.example.HonBam.paymentsapi.entity.SubscriptionInfo;
import com.example.HonBam.paymentsapi.entity.TossPaymentStatus;
import com.example.HonBam.paymentsapi.repository.PaidInfoRepository;
import com.example.HonBam.paymentsapi.repository.SubManagementRepository;
import com.example.HonBam.paymentsapi.repository.SubscriptionInfoRepository;
import com.example.HonBam.paymentsapi.repository.SubscriptionRepository;
import com.example.HonBam.userapi.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class SubscriptionManagementServiceTest {

    @Mock PaidInfoRepository paidInfoRepository;
    @Mock SubManagementRepository subManagementRepository;
    @Mock SubscriptionRepository subscriptionRepository;
    @Mock SubscriptionInfoRepository subscriptionInfoRepository;

    @InjectMocks SubscriptionManagementService service;

    private final User user = User.builder().id("user-1").userName("혼밤유저").build();

    private final Subscription monthlySub = Subscription.builder()
            .subId(1L).period(30).price(10000).orderName("1개월 구독").build();

    private TossPaymentApiResponse apiResponse(TossPaymentStatus status) {
        return TossPaymentApiResponse.builder()
                .paymentKey("pk_123")
                .orderId("order-1")
                .amount(10000)
                .orderName("1개월 구독")
                .method("카드")
                .requestedAt(LocalDateTime.of(2024, 1, 1, 10, 0))
                .paymentStatus(status)
                .build();
    }

    private PaidInfo paidInfo(LocalDateTime requestedAt) {
        return PaidInfo.builder()
                .paidId(5L)
                .orderId("order-1")
                .orderName("1개월 구독")
                .amount(10000)
                .method("카드")
                .requestedAt(requestedAt)
                .paymentStatus(TossPaymentStatus.DONE)
                .user(user)
                .build();
    }

    @Test
    @DisplayName("completePayment: DONE이면 SubManagement 생성 + SubscriptionInfo 만료일 갱신")
    void completePaymentCreatesSubscriptionWhenDone() {
        given(paidInfoRepository.save(any(PaidInfo.class))).willAnswer(inv -> inv.getArgument(0));
        given(subscriptionRepository.findByOrderName("1개월 구독")).willReturn(Optional.of(monthlySub));
        LocalDateTime paidAt = LocalDateTime.of(2024, 1, 1, 10, 0);
        given(subManagementRepository.findByUserIdWithFetchJoin("user-1"))
                .willReturn(Collections.singletonList(SubManagement.of(monthlySub, paidInfo(paidAt))));
        given(subscriptionInfoRepository.findByUserId("user-1")).willReturn(Optional.empty());

        PaidInfo result = service.completePayment(user, apiResponse(TossPaymentStatus.DONE));

        assertThat(result.getPaymentStatus()).isEqualTo(TossPaymentStatus.DONE);
        then(subManagementRepository).should().save(any(SubManagement.class));

        ArgumentCaptor<SubscriptionInfo> captor = ArgumentCaptor.forClass(SubscriptionInfo.class);
        then(subscriptionInfoRepository).should().save(captor.capture());
        assertThat(captor.getValue().getSubInfoId()).isNull();
        assertThat(captor.getValue().getDueDate()).isEqualTo(paidAt.plusDays(30));
    }

    @Test
    @DisplayName("completePayment: 가상계좌(WAITING_FOR_DEPOSIT)는 PaidInfo만 저장하고 구독은 만들지 않는다")
    void completePaymentOnlySavesPaidInfoForVirtualAccount() {
        given(paidInfoRepository.save(any(PaidInfo.class))).willAnswer(inv -> inv.getArgument(0));

        PaidInfo result = service.completePayment(user, apiResponse(TossPaymentStatus.WAITING_FOR_DEPOSIT));

        assertThat(result.getPaymentStatus()).isEqualTo(TossPaymentStatus.WAITING_FOR_DEPOSIT);
        then(subscriptionRepository).should(never()).findByOrderName(any());
        then(subManagementRepository).should(never()).save(any());
        then(subscriptionInfoRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("completePayment: 미등록 orderName이면 SubscriptionNotFoundException")
    void completePaymentRejectsUnknownOrderName() {
        given(paidInfoRepository.save(any(PaidInfo.class))).willAnswer(inv -> inv.getArgument(0));
        given(subscriptionRepository.findByOrderName("1개월 구독")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.completePayment(user, apiResponse(TossPaymentStatus.DONE)))
                .isInstanceOf(SubscriptionNotFoundException.class);

        then(subManagementRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("applyCancellation: 기존 행의 상태만 변경한다 (신규 INSERT 없음)")
    void applyCancellationUpdatesExistingRow() {
        PaidInfo existing = paidInfo(LocalDateTime.of(2024, 1, 1, 10, 0));
        given(paidInfoRepository.findById(5L)).willReturn(Optional.of(existing));

        PaidInfo result = service.applyCancellation(5L, TossPaymentStatus.CANCELED);

        assertThat(result.getPaymentStatus()).isEqualTo(TossPaymentStatus.CANCELED);
        assertThat(result).isSameAs(existing);
        then(paidInfoRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("getExpireDate: 복수 구독은 최초 결제일 + 기간 합산")
    void expireDateSumsPeriodsFromEarliestPayment() {
        Subscription quarterlySub = Subscription.builder()
                .subId(2L).period(90).price(25000).orderName("3개월 구독").build();
        LocalDateTime earliest = LocalDateTime.of(2024, 1, 1, 10, 0);
        List<SubManagement> subs = Arrays.asList(
                SubManagement.of(monthlySub, paidInfo(LocalDateTime.of(2024, 2, 1, 10, 0))),
                SubManagement.of(quarterlySub, paidInfo(earliest))
        );
        given(subManagementRepository.findByUserIdWithFetchJoin("user-1")).willReturn(subs);

        LocalDateTime expireDate = service.getExpireDate("user-1");

        assertThat(expireDate).isEqualTo(earliest.plusDays(30 + 90));
    }

    @Test
    @DisplayName("getExpireDate: 구독 내역이 없으면 NPE 대신 SubscriptionNotFoundException")
    void expireDateRejectsEmptySubscriptions() {
        given(subManagementRepository.findByUserIdWithFetchJoin("user-1")).willReturn(Collections.emptyList());

        assertThatThrownBy(() -> service.getExpireDate("user-1"))
                .isInstanceOf(SubscriptionNotFoundException.class);
    }

}
