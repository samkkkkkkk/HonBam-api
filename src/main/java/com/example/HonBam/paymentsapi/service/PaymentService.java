package com.example.HonBam.paymentsapi.service;

import com.example.HonBam.auth.TokenUserInfo;
import com.example.HonBam.exception.CustomUnauthorizedException;
import com.example.HonBam.exception.InvalidPaymentAmountException;
import com.example.HonBam.exception.OrderNotFoundException;
import com.example.HonBam.exception.PaymentAccessDeniedException;
import com.example.HonBam.exception.UserNotFoundException;
import com.example.HonBam.paymentsapi.client.TossPaymentsClient;
import com.example.HonBam.paymentsapi.client.dto.TossPaymentApiResponse;
import com.example.HonBam.paymentsapi.dto.request.PaymentConfirmReqDTO;
import com.example.HonBam.paymentsapi.dto.request.PaymentInfoRequestDTO;
import com.example.HonBam.paymentsapi.dto.response.TossPaymentResponseDTO;
import com.example.HonBam.paymentsapi.entity.PaidInfo;
import com.example.HonBam.paymentsapi.entity.PaymentInfo;
import com.example.HonBam.paymentsapi.entity.Subscription;
import com.example.HonBam.paymentsapi.repository.PaidInfoRepository;
import com.example.HonBam.paymentsapi.repository.PaymentInfoRepository;
import com.example.HonBam.paymentsapi.repository.SubscriptionRepository;
import com.example.HonBam.userapi.entity.User;
import com.example.HonBam.userapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

// 결제 오케스트레이션: 검증 → Toss API 호출(트랜잭션 밖) → 영속화는 SubscriptionManagementService에 위임
@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {

    private static final String DEFAULT_CANCEL_REASON = "단순 변심";

    private final UserRepository userRepository;
    private final PaymentInfoRepository paymentInfoRepository;
    private final PaidInfoRepository paidInfoRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final TossPaymentsClient tossPaymentsClient;
    private final SubscriptionManagementService subscriptionManagementService;

    private User findUserByToken(TokenUserInfo userInfo) {
        if (userInfo == null || userInfo.getUserId() == null) {
            throw new CustomUnauthorizedException("인증 정보가 유효하지 않습니다.");
        }
        return userRepository.findById(userInfo.getUserId())
                .orElseThrow(() -> new UserNotFoundException("회원 조회에 실패했습니다!"));
    }

    // 승인 요청 전 결제 정보 저장
    public void savePaymentInfo(PaymentInfoRequestDTO requestDTO, TokenUserInfo userInfo) {
        findUserByToken(userInfo);
        Optional<PaymentInfo> foundOrderId = paymentInfoRepository.findByOrderId(requestDTO.getOrderId());
        log.info("결제요청 들어왔다! {}", requestDTO);
        Long payId = foundOrderId.map(PaymentInfo::getPayId).orElse(null);
        paymentInfoRepository.save(requestDTO.toEntity(payId));
    }

    // 토스 결제 승인 요청
    public TossPaymentResponseDTO confirm(PaymentConfirmReqDTO requestDTO, TokenUserInfo userInfo) {
        User user = findUserByToken(userInfo);
        PaymentInfo foundPayment = paymentInfoRepository.findByOrderId(requestDTO.getOrderId()).orElseThrow(
                () -> new OrderNotFoundException("존재하지 않는 주문id입니다.")
        );

        if (requestDTO.getAmount() != foundPayment.getAmount()) {
            throw new InvalidPaymentAmountException("결제 금액이 주문 정보와 일치하지 않습니다.");
        }

        TossPaymentApiResponse tossPaymentApiResponse =
                tossPaymentsClient.confirm(requestDTO.getPaymentKey(), requestDTO.getOrderId(), requestDTO.getAmount());

        PaidInfo paidInfo = subscriptionManagementService.completePayment(user, tossPaymentApiResponse);
        return new TossPaymentResponseDTO(paidInfo);
    }

    public TossPaymentResponseDTO getOrderInfoByOrderId(String orderKey) {
        TossPaymentApiResponse order = tossPaymentsClient.getOrderByOrderId(orderKey);
        log.info("주문번호로 조회: {}", order);
        return TossPaymentResponseDTO.builder()
                .orderName(order.getOrderName())
                .paidAt(order.getRequestedAt())
                .orderId(order.getOrderId())
                .method(order.getMethod())
                .amount(order.getAmount() != null ? order.getAmount() : 0)
                .build();
    }

    // 토스 결제 취소 요청 — 본인 주문만 취소 가능
    public TossPaymentResponseDTO cancel(TokenUserInfo userInfo, PaymentConfirmReqDTO reqDTO) {
        User user = findUserByToken(userInfo);
        PaidInfo paidInfo = paidInfoRepository.findByOrderId(reqDTO.getOrderId()).orElseThrow(
                () -> new OrderNotFoundException("존재하지 않는 주문id입니다.")
        );

        if (!paidInfo.getUser().getId().equals(user.getId())) {
            throw new PaymentAccessDeniedException("본인의 주문만 취소할 수 있습니다.");
        }

        TossPaymentApiResponse cancelResponse = tossPaymentsClient.cancel(reqDTO.getPaymentKey(), DEFAULT_CANCEL_REASON);
        log.info("취소 응답: {}", cancelResponse);

        PaidInfo canceled = subscriptionManagementService.applyCancellation(paidInfo.getPaidId(), cancelResponse.getPaymentStatus());
        return new TossPaymentResponseDTO(canceled);
    }

    public List<Subscription> getSubscriptions() {
        return subscriptionRepository.findAll();
    }

}
