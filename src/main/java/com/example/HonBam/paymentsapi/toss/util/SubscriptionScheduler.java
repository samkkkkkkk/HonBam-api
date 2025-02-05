package com.example.HonBam.paymentsapi.toss.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SubscriptionScheduler {

    @Autowired
    private SubscriptionService subscriptionService;

    @Scheduled(fixedDelay = 100000000)
    public void updateSubscription() {
        subscriptionService.updateSubscriptions2();
        log.info("updateSubscription 실행");
    }


}
