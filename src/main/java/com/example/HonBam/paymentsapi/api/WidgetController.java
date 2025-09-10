package com.example.HonBam.paymentsapi.api;

import com.example.HonBam.auth.CustomUserDetails;
import com.example.HonBam.auth.TokenUserInfo;
import com.example.HonBam.config.TossPaymentsConfig;
import com.example.HonBam.paymentsapi.dto.request.PaymentConfirmReqDTO;
import com.example.HonBam.paymentsapi.dto.request.PaymentInfoRequestDTO;
import com.example.HonBam.paymentsapi.dto.response.TossPaymentResponseDTO;
import com.example.HonBam.paymentsapi.entity.Subscription;
import com.example.HonBam.paymentsapi.service.TossService;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@Slf4j
@RequestMapping("/api/tosspay")
@RequiredArgsConstructor
public class WidgetController {


    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final TossPaymentsConfig tossPaymentsConfig;
    private final TossService tossService;



    // 구독권 리스트
    @GetMapping("/subscription")
    public ResponseEntity<?> subscriptionList() {
        List<Subscription> subList = tossService.getSubscriptions();
        return ResponseEntity.ok().body(subList);
    }


    // 승인 전 주문정보
    @PostMapping("/info")
    public ResponseEntity<?> paymentInfo(@AuthenticationPrincipal CustomUserDetails userDetails,
                                         @RequestBody PaymentInfoRequestDTO requestDTO) {
        log.info("/api/tosspay/info 요청이 들어옴");
        tossService.savePaymentInfo(requestDTO, userDetails);
        return ResponseEntity.ok().body("ok");
    }

    // 결제 승인 요청
    @PostMapping("/confirm")
    public @ResponseBody ResponseEntity<?> confirmPayment(@RequestBody PaymentConfirmReqDTO requestDTO,
                                                @AuthenticationPrincipal CustomUserDetails userDetails ) {
        try {
           TossPaymentResponseDTO confirmDTO = tossService.confirm(requestDTO, userDetails);
            return ResponseEntity.ok().body(confirmDTO);
        } catch (JsonProcessingException e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }



    @PostMapping("/cancel")
    public ResponseEntity<?> tossCancel(@AuthenticationPrincipal CustomUserDetails userDetails,
                                        @RequestBody  PaymentConfirmReqDTO reqDTO) {
        log.info("/cancel 요청이 들어옴");
        TossPaymentResponseDTO responseDTO = null;
        try {
            responseDTO = tossService.cancel(userDetails, reqDTO);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return ResponseEntity.ok().body(responseDTO);
    }

    // 주문정보 조회 요청
    @PostMapping("/order/{orderKey}")
    public ResponseEntity<?> tossOrder(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                            @PathVariable(value = "orderKey") String orderKey) {
        TossPaymentResponseDTO responseDTO = tossService.getOrderInfoByOrderId(orderKey);
        try {
            return ResponseEntity.ok().body(responseDTO);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * 인증성공처리
     *
     * @param request
     * @param model
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/success", method = RequestMethod.GET)
    public String paymentRequest(HttpServletRequest request, Model model) throws Exception {
        log.info("success요청이 들어옴!");
        return "/success";
    }

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String index(HttpServletRequest request, Model model) throws Exception {
        return "/checkout";
    }

    /**
     * 인증실패처리
     *
     * @param request
     * @param model
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/fail", method = RequestMethod.GET)
    public String failPayment(HttpServletRequest request, Model model) throws Exception {
        String failCode = request.getParameter("code");
        String failMessage = request.getParameter("message");

        model.addAttribute("code", failCode);
        model.addAttribute("message", failMessage);

        return "/fail";
    }



}
