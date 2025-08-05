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
    public ResponseEntity<?> paymentInfo(@AuthenticationPrincipal TokenUserInfo userInfo,
                                         @RequestBody PaymentInfoRequestDTO requestDTO) {
        log.info("/api/tosspay/info 요청이 들어옴");
        tossService.savePaymentInfo(requestDTO, userInfo);
        return ResponseEntity.ok().body("ok");
    }

    // 결제 승인 요청
    @PostMapping("/confirm")
    public @ResponseBody ResponseEntity<?> confirmPayment(@RequestBody PaymentConfirmReqDTO requestDTO,
                                                @AuthenticationPrincipal TokenUserInfo userInfo ) {
        try {
           TossPaymentResponseDTO confirmDTO = tossService.confirm(requestDTO, userInfo);
            return ResponseEntity.ok().body(confirmDTO);
        } catch (JsonProcessingException e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

//    @RequestMapping(value = "/confirm")
//    public ResponseEntity<JSONObject> confirmPayment(@RequestBody String jsonBody) throws Exception {
//
//        System.out.println("/confirm요청");
//
//        JSONParser parser = new JSONParser();
//        String orderId;
//        String amount;
//        String paymentKey;
//        try {
//            // 클라이언트에서 받은 JSON 요청 바디입니다.
//            JSONObject requestData = (JSONObject) parser.parse(jsonBody);
//            System.out.println("amount = " + requestData.get("amount"));
//            System.out.println("orderId = " + requestData.get("orderId"));
//            System.out.println("paymentKey = " + requestData.get("paymentKey"));
//            paymentKey = (String) requestData.get("paymentKey");
//            System.out.println("requestData.get() = " + (String) requestData.get("paymentKey"));
//            orderId = (String) requestData.get("orderId");
//            amount = (String) requestData.get("amount");
//        } catch (ParseException e) {
//            throw new RuntimeException(e);
//        }
//        ;
//        JSONObject obj = new JSONObject();
//        obj.put("orderId", orderId);
//        obj.put("amount", amount);
//        obj.put("paymentKey", paymentKey);
//
//        // TODO: 개발자센터에 로그인해서 내 결제위젯 연동 키 > 시크릿 키를 입력하세요. 시크릿 키는 외부에 공개되면 안돼요.
//        // @docs https://docs.tosspayments.com/reference/using-api/api-keys
//        String widgetSecretKey = tossPaymentsConfig.getTossSecretKey();
//
//        // 토스페이먼츠 API는 시크릿 키를 사용자 ID로 사용하고, 비밀번호는 사용하지 않습니다.
//        // 비밀번호가 없다는 것을 알리기 위해 시크릿 키 뒤에 콜론을 추가합니다.
//        // @docs https://docs.tosspayments.com/reference/using-api/authorization#%EC%9D%B8%EC%A6%9D
//        Base64.Encoder encoder = Base64.getEncoder();
//        byte[] encodedBytes = encoder.encode((widgetSecretKey + ":").getBytes(StandardCharsets.UTF_8));
//        String authorizations = "Basic " + new String(encodedBytes);
//
//        // 결제 승인 API를 호출하세요.
//        // 결제를 승인하면 결제수단에서 금액이 차감돼요.
//        // @docs https://docs.tosspayments.com/guides/v2/payment-widget/integration#3-결제-승인하기
//        URL url = new URL("https://api.tosspayments.com/v1/payments/confirm");
//        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
//        connection.setRequestProperty("Authorization", authorizations);
//        connection.setRequestProperty("Content-Type", "application/json");
//        connection.setRequestMethod("POST");
//        connection.setDoOutput(true);
//
//
//        OutputStream outputStream = connection.getOutputStream();
//        outputStream.write(obj.toString().getBytes("UTF-8"));
//
//        int code = connection.getResponseCode();
//        boolean isSuccess = code == 200;
//
//        InputStream responseStream = isSuccess ? connection.getInputStream() : connection.getErrorStream();
//
//        // TODO: 결제 성공 및 실패 비즈니스 로직을 구현하세요.
//        Reader reader = new InputStreamReader(responseStream, StandardCharsets.UTF_8);
//        JSONObject jsonObject = (JSONObject) parser.parse(reader);
//        responseStream.close();
//
//        return ResponseEntity.status(code).body(jsonObject);
//    }

    @PostMapping("/cancel")
    public ResponseEntity<?> tossCancel(@AuthenticationPrincipal TokenUserInfo userInfo,
                                        @RequestBody  PaymentConfirmReqDTO reqDTO) {
        log.info("/cancel 요청이 들어옴");
        TossPaymentResponseDTO responseDTO = null;
        try {
            responseDTO = tossService.cancel(userInfo, reqDTO);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return ResponseEntity.ok().body(responseDTO);
    }

    // 주문정보 조회 요청
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
