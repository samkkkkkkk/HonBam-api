package com.example.HonBam.paymentsapi.service;

import com.example.HonBam.exception.OrderNotFoundException;
import com.example.HonBam.exception.SubscriptionNotFoundException;
import com.example.HonBam.paymentsapi.client.dto.TossPaymentApiResponse;
import com.example.HonBam.paymentsapi.entity.*;
import com.example.HonBam.paymentsapi.repository.PaidInfoRepository;
import com.example.HonBam.paymentsapi.repository.SubManagementRepository;
import com.example.HonBam.paymentsapi.repository.SubscriptionInfoRepository;
import com.example.HonBam.paymentsapi.repository.SubscriptionRepository;
import com.example.HonBam.userapi.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

// 결제 결과의 영속화 전담 — 외부 API 호출 없이 DB 트랜잭션만 담당
// (util/SubscriptionService와 빈 이름 충돌을 피하기 위해 이 이름 사용)
@Service
@Slf4j
@RequiredArgsConstructor
public class SubscriptionManagementService {

    private final PaidInfoRepository paidInfoRepository;
    private final SubManagementRepository subManagementRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionInfoRepository subscriptionInfoRepository;

    // 결제 승인 결과 저장. status가 DONE이면 구독 생성 + 만료일 갱신까지 원자적으로 처리
    @Transactional
    public PaidInfo completePayment(User user, TossPaymentApiResponse response) {
        PaidInfo paidInfo = paidInfoRepository.save(response.toEntity(user));

        if (paidInfo.getPaymentStatus() == TossPaymentStatus.DONE) {
            Subscription subscription = subscriptionRepository.findByOrderName(paidInfo.getOrderName()).orElseThrow(
                    () -> new SubscriptionNotFoundException("구독권이 존재하지 않습니다.")
            );
            subManagementRepository.save(SubManagement.of(subscription, paidInfo));

            Long subInfoId = subscriptionInfoRepository.findByUserId(user.getId())
                    .map(SubscriptionInfo::getSubInfoId)
                    .orElse(null);
            subscriptionInfoRepository.save(SubscriptionInfo.builder()
                    .subInfoId(subInfoId)
                    .dueDate(getExpireDate(user.getId()))
                    .user(user)
                    .build());
        }
        return paidInfo;
    }

    // 취소 상태 반영 — 기존 행을 dirty checking으로 UPDATE (중복 INSERT 방지)
    @Transactional
    public PaidInfo applyCancellation(Long paidId, TossPaymentStatus paymentStatus) {
        PaidInfo paidInfo = paidInfoRepository.findById(paidId).orElseThrow(
                () -> new OrderNotFoundException("존재하지 않는 주문입니다.")
        );
        paidInfo.applyCancellation(paymentStatus);
        return paidInfo;
    }

    // 만료일 = 최초 결제일 + 보유 구독 기간 합산
    public LocalDateTime getExpireDate(final String userId) {
        List<SubManagement> subManagements = subManagementRepository.findByUserIdWithFetchJoin(userId);
        if (subManagements.isEmpty()) {
            throw new SubscriptionNotFoundException("구독 내역이 존재하지 않습니다.");
        }
        int period = 0;
        LocalDateTime paidDate = null;
        for (SubManagement s : subManagements) {
            period += s.getSubscription().getPeriod();
            if (paidDate == null || s.getPaidInfo().getRequestedAt().isBefore(paidDate)) {
                paidDate = s.getPaidInfo().getRequestedAt();
            }
        }
        return paidDate.plusDays(period);
    }

}
