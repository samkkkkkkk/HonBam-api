package com.example.HonBam.paymentsapi.service;

import com.example.HonBam.auth.TokenUserInfo;
import com.example.HonBam.config.TossPaymentsConfig;
import com.example.HonBam.exception.OrderNotFoundException;
import com.example.HonBam.exception.SubscriptionNotFoundException;
import com.example.HonBam.exception.UserNotFoundException;
import com.example.HonBam.paymentsapi.dto.request.PaymentConfirmReqDTO;
import com.example.HonBam.paymentsapi.dto.request.PaymentInfoRequestDTO;
import com.example.HonBam.paymentsapi.dto.request.SubManagementReqDTO;
import com.example.HonBam.paymentsapi.dto.request.TosspaymentRequestDTO;
import com.example.HonBam.paymentsapi.dto.response.TossPaymentResponseDTO;
import com.example.HonBam.paymentsapi.entity.*;
import com.example.HonBam.paymentsapi.repository.*;
import com.example.HonBam.userapi.entity.User;
import com.example.HonBam.userapi.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class TossService {

    private final UserRepository userRepository;
    private final PaymentInfoRepository paymentInfoRepository;
    private final TossPaymentsConfig tossPaymentsConfig;
    private final PaidInfoRepository paidInfoRepository;
    private final SubManagementRepository subManagementRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionInfoRepository subscriptionInfoRepository;

    private User findUserByToken(TokenUserInfo userInfo) {
        if (userInfo == null || userInfo.getUserId() == null) {
            throw new RuntimeException("인증 정보가 유효하지 않습니다.");
        }
        return userRepository.findById(userInfo.getUserId())
                .orElseThrow(() -> new UserNotFoundException("회원 조회에 실패했습니다!"));
    }

    // 승인 요청 전 결제 정보 저장
    public void savePaymentInfo(PaymentInfoRequestDTO requestDTO, TokenUserInfo userInfo) {
        User user = findUserByToken(userInfo);
        Optional<PaymentInfo> foundOrderId = paymentInfoRepository.findByOrderId(requestDTO.getOrderId());
        log.info("결제요청 들어왔다! {}", requestDTO);
        Long payId = null;
        if (foundOrderId.isPresent()) {
            payId = foundOrderId.get().getPayId();
        }
        paymentInfoRepository.save(requestDTO.toEntity(payId));
    }

    // 토스 결제 승인 요청
    public TossPaymentResponseDTO confirm(PaymentConfirmReqDTO requestDTO, TokenUserInfo userInfo) throws JsonProcessingException {
        String authorizations = getEncodedKey(tossPaymentsConfig.getTossSecretKey());
        int amount = requestDTO.getAmount();
        String orderId = requestDTO.getOrderId();
        String paymentKey = requestDTO.getPaymentKey();

        PaidInfo paidInfo;
        PaymentInfo foundPayment = paymentInfoRepository.findPaymentByOrderId(requestDTO.getOrderId());

        if (amount != foundPayment.getAmount() || !orderId.equals(foundPayment.getOrderId())) {
            throw new IllegalArgumentException("주문 정보가 일치하지 않습니다.");
        }

        TosspaymentRequestDTO tosspaymentRequestDTO = getMapResponseEntity(orderId, paymentKey, amount, authorizations);
        User user = findUserByToken(userInfo);

        if (tosspaymentRequestDTO.getMethod().equals("가상계좌")) {
            paidInfo = tosspaymentRequestDTO.toEntityVirtualAccount(user);
        } else {
            paidInfo = tosspaymentRequestDTO.toEntity(user);
        }

        PaidInfo save = paidInfoRepository.save(paidInfo);

        if (save.getPaymentStatus().equals("DONE")) {
            Subscription subscription = subscriptionRepository.findByOrderName(save.getOrderName()).orElseThrow(
                    () -> new SubscriptionNotFoundException("구독권이 존재하지 않습니다.")
            );
            subManagementRepository.save(new SubManagementReqDTO().toEntity(subscription, save));

            Long subInfoId = null;
            Optional<SubscriptionInfo> foundSubInfoId = subscriptionInfoRepository.findByUserId(user.getId());
            if (foundSubInfoId.isPresent()) {
                subInfoId = foundSubInfoId.get().getSubInfoId();
            }
            subscriptionInfoRepository.save(SubscriptionInfo.builder()
                    .subInfoId(subInfoId)
                    .dueDate(getExpireDate(user.getId()))
                    .user(user)
                    .build());
        }
        return new TossPaymentResponseDTO(save);
    }

    public TossPaymentResponseDTO getOrderInfoByOrderId(String orderKey) {
        String requestURI = "https://api.tosspayments.com/v1/payments/orders/{orderId}";
        String authorizations = getEncodedKey(tossPaymentsConfig.getTossSecretKey());
        WebClient webClient = WebClient.create();
        TossPaymentResponseDTO responseData = webClient.get()
                .uri(requestURI, orderKey)
                .header("Authorization", authorizations)
                .retrieve()
                .bodyToMono(TossPaymentResponseDTO.class)
                .block();
        log.info("주문번호로 조회: {}", responseData);
        return responseData;
    }

    public TossPaymentResponseDTO getOrderInfo(String orderKey) {
        PaidInfo paidInfo = paidInfoRepository.findByOrderId(orderKey).orElseThrow(
                () -> new OrderNotFoundException("존재하지 않는 주문 번호입니다.")
        );
        String requestURI = "https://api.tosspayments.com/v1/payments/{paymentKey}";
        String authorizations = getEncodedKey(tossPaymentsConfig.getTossSecretKey());
        WebClient webClient = WebClient.create();
        TossPaymentResponseDTO responseData = webClient.get()
                .uri(requestURI, orderKey)
                .header("Authorization", authorizations)
                .retrieve()
                .bodyToMono(TossPaymentResponseDTO.class)
                .block();
        if (!paidInfo.getOrderId().equals(responseData.getOrderId())) {
            throw new IllegalArgumentException("잘못된 정보입니다.");
        }
        log.info("주문 조회: {}", responseData);
        return responseData;
    }

    private TosspaymentRequestDTO getMapResponseEntity(String orderId, String paymentKey, int amount, String authorizations) {
        String requestURI = "https://api.tosspayments.com/v1/payments/confirm";
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", authorizations);
        headers.add("Content-Type", "application/json");

        Map<String, Object> params = new HashMap<>();
        params.put("orderId", orderId);
        params.put("paymentKey", paymentKey);
        params.put("amount", amount);

        HttpEntity<Object> requestEntity = new HttpEntity<>(params, headers);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<TosspaymentRequestDTO> responseEntity = restTemplate.exchange(requestURI, HttpMethod.POST, requestEntity, TosspaymentRequestDTO.class);
        TosspaymentRequestDTO responseData = responseEntity.getBody();
        log.info("데이터: {}", responseEntity);
        log.info("토스페이 승인: {}", responseData);
        return responseData;
    }

    public TossPaymentResponseDTO cancel(TokenUserInfo userInfo, PaymentConfirmReqDTO reqDTO) throws JsonProcessingException {
        User user = findUserByToken(userInfo);
        PaidInfo paidInfo = paidInfoRepository.findByOrderId(reqDTO.getOrderId()).orElseThrow(
                () -> new OrderNotFoundException("존재하지 않는 주문id입니다.")
        );
        String authorizations = getEncodedKey(tossPaymentsConfig.getTossSecretKey());
        String paymentKey = reqDTO.getPaymentKey();
        String requestURI = "https://api.tosspayments.com/v1/payments/{paymentKey}/cancel";
        log.info("환불요청 보냄");

        Map<String, Object> params = new HashMap<>();
        params.put("cancelReason", "단순 변심");

        Consumer<HttpHeaders> headers = httpHeaders -> {
            httpHeaders.add("Content-Type", "application/json");
            httpHeaders.add("Authorization", authorizations);
        };

        WebClient webClient = WebClient.create();
        TosspaymentRequestDTO requestDTO = webClient.post()
                .uri(requestURI, paymentKey)
                .headers(headers)
                .bodyValue(params.toString())
                .retrieve()
                .bodyToMono(TosspaymentRequestDTO.class)
                .block();

        PaidInfo save = requestDTO.toEntity(user, paidInfo.getPaidId());
        PaidInfo saved = paidInfoRepository.save(save);
        log.info("블락: {}", requestDTO);
        return new TossPaymentResponseDTO(saved);
    }

    private static String getEncodedKey(String tossSecretKey) {
        return "Basic " + new String(Base64.getEncoder().encode((tossSecretKey + ":").getBytes(StandardCharsets.UTF_8)));
    }

    public List<Subscription> getSubscriptions() {
        return subscriptionRepository.findAll();
    }

    public LocalDateTime getExpireDate(final String userId) {
        Optional<List<SubManagement>> foundInfo = subManagementRepository.findByUserIdWithFetchJoin(userId);
        int period = 0;
        LocalDateTime paidDate = null;
        if (foundInfo.isPresent()) {
            List<SubManagement> subManagements = foundInfo.get();
            for (SubManagement s : subManagements) {
                period += s.getSubscription().getPeriod();
                if (paidDate == null || s.getPaidInfo().getRequestedAt().isBefore(paidDate)) {
                    paidDate = s.getPaidInfo().getRequestedAt();
                }
            }
        }
        return Objects.requireNonNull(paidDate).plusDays(period);
    }
}
