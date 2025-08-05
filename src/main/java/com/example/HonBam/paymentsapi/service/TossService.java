package com.example.HonBam.paymentsapi.service;

import com.example.HonBam.auth.TokenUserInfo;
import com.example.HonBam.config.TossPaymentsConfig;
import com.example.HonBam.paymentsapi.dto.request.PaymentConfirmReqDTO;
import com.example.HonBam.paymentsapi.dto.request.PaymentInfoRequestDTO;
import com.example.HonBam.paymentsapi.dto.request.SubManagementReqDTO;
import com.example.HonBam.paymentsapi.dto.request.TosspaymentRequestDTO;
import com.example.HonBam.paymentsapi.dto.response.TossPaymentResponseDTO;
import com.example.HonBam.paymentsapi.entity.*;
import com.example.HonBam.paymentsapi.repository.*;
import com.example.HonBam.userapi.entity.User;
import com.example.HonBam.userapi.repository.UserRepository;
import com.example.HonBam.exception.UserNotFoundException;
import com.example.HonBam.exception.OrderNotFoundException;
import com.example.HonBam.exception.SubscriptionNotFoundException;
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


    // 승인 요청 전 결제 정보 저장
    public void savePaymentInfo(PaymentInfoRequestDTO requestDTO, TokenUserInfo userInfo) {
        // orderId로 DB에서 조회하기
        Optional<PaymentInfo> foundOrderId = paymentInfoRepository.findByOrderId(requestDTO.getOrderId());
        log.info("결제요청 들어왔다! {}", requestDTO);
        Long payId = null; // 기존 주문번호가 없다면 payId는 null로 지정

        // paymentInfo 객체가 존재 한다면 payId를 기존 객체의 id로 변경
        if (foundOrderId.isPresent()) {
            payId = foundOrderId.get().getPayId();
        }

        // 기존 orderId가 존재 한다면 update, 아니면 insert 진행
        paymentInfoRepository.save(requestDTO.toEntity(payId));

    }

    // 토스 결제 승인 요청
    public TossPaymentResponseDTO confirm(PaymentConfirmReqDTO requestDTO, TokenUserInfo userInfo) throws JsonProcessingException {
        String authorizations = getEncodedKey(tossPaymentsConfig.getTossSecretKey());
        // 요청 데이터
        int amount = requestDTO.getAmount();
        String orderId = requestDTO.getOrderId();
        String paymentKey = requestDTO.getPaymentKey();

        PaidInfo paidInfo;

        // DB에 저장 되어있는 결제 정보와 요청이 들어온 결제 정보가 같은 지 확인
        PaymentInfo foundPayment = paymentInfoRepository.findPaymentByOrderId(requestDTO.getOrderId());

        if (amount != foundPayment.getAmount()
                || !orderId.equals(foundPayment.getOrderId())) {
            throw new IllegalArgumentException("주문 정보가 일치하지 않습니다.");
        }

        log.info("결제정보 확인 amount: {}, orderId: {}, paymentKey: {}", amount, orderId, paymentKey);

        // 결제 승인 정보 가져오기
        TosspaymentRequestDTO tosspaymentRequestDTO = getMapResponseEntity(orderId, paymentKey, amount, authorizations);

        // User 정보 조회
        User user = userRepository.findById(userInfo.getEmail()).orElseThrow(
                () -> new UserNotFoundException("회원이 존재하지 않습니다.")
        );

        // 주문정보 저장
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

            SubManagement savedSubscription = subManagementRepository.save(new SubManagementReqDTO().toEntity(subscription, save));
            log.info("구독권 관리: {}", savedSubscription);

            Long subInfoId = null;
            Optional<SubscriptionInfo> foundSubInfoId = subscriptionInfoRepository.findByUserId(user.getId());
            if(foundSubInfoId.isPresent()) {
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


    // orderId로 주문 내역 조회
    public TossPaymentResponseDTO getOrderInfoByOrderId(String orderKey) {

        // 요청 uri
        String requestURI = "https://api.tosspayments.com/v1/payments" + "/orders/{orderId}";

        // SecretKey
        String tossSecretKey = tossPaymentsConfig.getTossSecretKey();

        // Base64로 인코딩하기
        String authorizations = getEncodedKey(tossSecretKey);

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

    // 주문 조회하기
    public TossPaymentResponseDTO getOrderInfo(String orderKey) {

        PaidInfo paidInfo = paidInfoRepository.findByOrderId(orderKey).orElseThrow(
                () -> new OrderNotFoundException("존재하지 않는 주문 번호입니다.")
        );

        // 요청 URI
        String requestURI = "https://api.tosspayments.com/v1/payments" + "/{paymentKey}";

        // SecretKey
        String tossSecretKey = tossPaymentsConfig.getTossSecretKey();

        // Base64로 인코딩하기
        String authorizations = getEncodedKey(tossSecretKey);

        // 요청보내기
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

    // 토스 페이먼츠에 승인 요청 보내기
    private TosspaymentRequestDTO getMapResponseEntity(String orderId, String paymentKey, int amount, String authorizations) {

        // 요청 URI
        String requestURI = "https://api.tosspayments.com/v1/payments" + "/confirm";

        // SecretKey
        String tossSecretKey = tossPaymentsConfig.getTossSecretKey();

        // Base64로 인코딩하기
        //        String authorizations = getEncodedKey(tossSecretKey);

        // 헤더 설정하기
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", authorizations);
        headers.add("Content-Type", "application/json");

        // 요청 파라미터(바디) 설정 토스 페이먼츠는 JSON형태로 통신
        Map<String, Object> params = new HashMap<>();
        params.put("orderId", orderId);
        params.put("paymentKey", paymentKey);
        params.put("amount", amount);

        // Map 데이터를 JSON데이터로 직렬화
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            String jsonString = objectMapper.writeValueAsString(params);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        // 헤더와 바디 정보를 합치기 위해 HttpEntity 생성
        HttpEntity<Object> requestEntity = new HttpEntity<>(params, headers);

        // 토스 페이먼츠와 통신
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<TosspaymentRequestDTO> responseEntity = restTemplate.exchange(requestURI, HttpMethod.POST, requestEntity, TosspaymentRequestDTO.class);
        TosspaymentRequestDTO responseData = responseEntity.getBody();
        log.info("데이터: {}", responseEntity);
        log.info("토스페이 승인: {}", responseData);

        return responseData;
    }


    // 결제 취소
    // /v1/payments/{paymentKey}/cancel
    public TossPaymentResponseDTO cancel(TokenUserInfo userInfo, PaymentConfirmReqDTO reqDTO) throws JsonProcessingException {

        // 유저 정보 가져오기
        User user = userRepository.findById(userInfo.getEmail()).orElseThrow(
                () -> new UserNotFoundException("존재하지 않는 유저 입니다.")
        );

        // payId 찾기
        PaidInfo paidInfo = paidInfoRepository.findByOrderId(reqDTO.getOrderId()).orElseThrow(
                () -> new OrderNotFoundException("존재하지 않는 주문id입니다.")
        );

        String authorizations = getEncodedKey(tossPaymentsConfig.getTossSecretKey());

        String paymentKey = reqDTO.getPaymentKey();
        String requestURI = "https://api.tosspayments.com/v1/payments/{paymentKey}/cancel";

        log.info("환불요청 보냄");

        // 요청 바디
        Map<String, Object> params = new HashMap<>();
        params.put("cancelReason", "단순 변심");

        ObjectMapper objectMapper = new ObjectMapper();
        String jsonString = objectMapper.writeValueAsString(params);

        // 요청 헤더
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


    // 시크릿키 인코딩
    private static String getEncodedKey(String tossSecretKey) {
        Base64.Encoder encoder = Base64.getEncoder();
        byte[] encodeBytes = encoder.encode((tossSecretKey + ":").getBytes(StandardCharsets.UTF_8));
        return "Basic " + new String(encodeBytes);
    }


    // 구독권 정보
    public List<Subscription> getSubscriptions() {

        return subscriptionRepository.findAll();

    }

    // 구독 만료 날짜 계산
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

        LocalDateTime expiredDate = Objects.requireNonNull(paidDate).plusDays(period);
        return expiredDate;
    }
}
