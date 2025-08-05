package com.example.HonBam.config;


import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class TossPaymentsConfig {

    @Value("${payment.toss.test_client_api_key}")
    String tossClientKey;

    @Value("${payment.toss.test_secret_api_key}")
    String tossSecretKey;

    @Value("${payment.toss.success_url}")
    String tossSuccessUrl;

    @Value("${payment.toss.fail_url}")
    String tossFailUrl;

    @Getter
    private static final String TOSS_URl = "https://api.tosspayments.com/v1/payments";
    @Getter
    private static final String TOSS_CANCEL_URL = "https://api.tosspayments.com/v1/payments/{paymentKey}/cancel";
    @Getter
    private static final String TOSS_VIRTUAL_ACCOUNT = "https://api.tosspayments.com/v1/virtual-accounts";

}
