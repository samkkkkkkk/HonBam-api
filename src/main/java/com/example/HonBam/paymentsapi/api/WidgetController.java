package com.example.HonBam.paymentsapi.api;

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

    @GetMapping("/subscription")
    public ResponseEntity<?> subscriptionList() {
        List<Subscription> subList = tossService.getSubscriptions();
        return ResponseEntity.ok().body(subList);
    }

    @PostMapping("/info")
    public ResponseEntity<?> paymentInfo(@AuthenticationPrincipal TokenUserInfo userInfo,
                                         @RequestBody PaymentInfoRequestDTO requestDTO) {
        log.info("/api/tosspay/info 요청이 들어옴");
        tossService.savePaymentInfo(requestDTO, userInfo);
        return ResponseEntity.ok().body("ok");
    }

    @PostMapping("/confirm")
    public @ResponseBody ResponseEntity<?> confirmPayment(@RequestBody PaymentConfirmReqDTO requestDTO,
                                                @AuthenticationPrincipal TokenUserInfo userInfo) {
        try {
            TossPaymentResponseDTO confirmDTO = tossService.confirm(requestDTO, userInfo);
            return ResponseEntity.ok().body(confirmDTO);
        } catch (JsonProcessingException e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @PostMapping("/cancel")
    public ResponseEntity<?> tossCancel(@AuthenticationPrincipal TokenUserInfo userInfo,
                                        @RequestBody PaymentConfirmReqDTO reqDTO) {
        log.info("/cancel 요청이 들어옴");
        TossPaymentResponseDTO responseDTO = null;
        try {
            responseDTO = tossService.cancel(userInfo, reqDTO);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return ResponseEntity.ok().body(responseDTO);
    }

    @PostMapping("/order/{orderKey}")
    public ResponseEntity<?> tossOrder(@AuthenticationPrincipal TokenUserInfo userInfo,
                                       @PathVariable(value = "orderKey") String orderKey) {
        TossPaymentResponseDTO responseDTO = tossService.getOrderInfoByOrderId(orderKey);
        try {
            return ResponseEntity.ok().body(responseDTO);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @RequestMapping(value = "/success", method = RequestMethod.GET)
    public String paymentRequest(HttpServletRequest request, Model model) throws Exception {
        log.info("success요청이 들어옴!");
        return "/success";
    }

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String index(HttpServletRequest request, Model model) throws Exception {
        return "/checkout";
    }

    @RequestMapping(value = "/fail", method = RequestMethod.GET)
    public String failPayment(HttpServletRequest request, Model model) throws Exception {
        String failCode = request.getParameter("code");
        String failMessage = request.getParameter("message");
        model.addAttribute("code", failCode);
        model.addAttribute("message", failMessage);
        return "/fail";
    }
}
