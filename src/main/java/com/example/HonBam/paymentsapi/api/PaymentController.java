package com.example.HonBam.paymentsapi.api;

import com.example.HonBam.auth.TokenUserInfo;
import com.example.HonBam.paymentsapi.dto.request.PaymentConfirmReqDTO;
import com.example.HonBam.paymentsapi.dto.request.PaymentInfoRequestDTO;
import com.example.HonBam.paymentsapi.dto.response.SubscriptionResponseDTO;
import com.example.HonBam.paymentsapi.dto.response.TossPaymentResponseDTO;
import com.example.HonBam.paymentsapi.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@Slf4j
@RequestMapping("/api/tosspay")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/subscription")
    public ResponseEntity<?> subscriptionList() {
        List<SubscriptionResponseDTO> subList = paymentService.getSubscriptions().stream()
                .map(SubscriptionResponseDTO::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok().body(subList);
    }

    @PostMapping("/info")
    public ResponseEntity<?> paymentInfo(@AuthenticationPrincipal TokenUserInfo userInfo,
                                         @RequestBody PaymentInfoRequestDTO requestDTO) {
        log.info("/api/tosspay/info 요청이 들어옴");
        paymentService.savePaymentInfo(requestDTO, userInfo);
        return ResponseEntity.ok().body("ok");
    }

    @PostMapping("/confirm")
    public ResponseEntity<?> confirmPayment(@RequestBody PaymentConfirmReqDTO requestDTO,
                                            @AuthenticationPrincipal TokenUserInfo userInfo) {
        TossPaymentResponseDTO confirmDTO = paymentService.confirm(requestDTO, userInfo);
        return ResponseEntity.ok().body(confirmDTO);
    }

    @PostMapping("/cancel")
    public ResponseEntity<?> tossCancel(@AuthenticationPrincipal TokenUserInfo userInfo,
                                        @RequestBody PaymentConfirmReqDTO reqDTO) {
        log.info("/cancel 요청이 들어옴");
        TossPaymentResponseDTO responseDTO = paymentService.cancel(userInfo, reqDTO);
        return ResponseEntity.ok().body(responseDTO);
    }

    @PostMapping("/order/{orderKey}")
    public ResponseEntity<?> tossOrder(@AuthenticationPrincipal TokenUserInfo userInfo,
                                       @PathVariable(value = "orderKey") String orderKey) {
        TossPaymentResponseDTO responseDTO = paymentService.getOrderInfoByOrderId(orderKey);
        return ResponseEntity.ok().body(responseDTO);
    }

}
