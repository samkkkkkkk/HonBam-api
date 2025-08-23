package com.example.HonBam.util;

import com.example.HonBam.paymentsapi.entity.SubscriptionInfo;
import com.example.HonBam.paymentsapi.repository.SubscriptionInfoRepository;
import com.example.HonBam.userapi.entity.User;
import com.example.HonBam.userapi.entity.UserPay;
import com.example.HonBam.userapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionInfoRepository subscriptionInfoRepository;
    private final UserRepository userRepository;
//    private final WebSocketNotifier webSocketNotifier;
//    public void updateSubscriptions() {
//        List<SubscriptionInfo> expireDateList = subscriptionInfoRepository.findExpireDate();
//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
//
//        for (SubscriptionInfo s : expireDateList) {
//            System.out.println("s = " + s.getDueDate());
//            if (s.getDueDate().isBefore(LocalDateTime.parse("2026-02-07 19:45:23", formatter))) {
//                Optional<User> user = userRepository.findById(s.getUser().getId());
//                user.ifPresent(u -> {
//                    u.setUserPay(UserPay.NORMAL);
//                    userRepository.save(u);
//                });
//
//                // websocket으로 메세지 전송
////                webSocketNotifier.notifyClients("안녕ㅎㅎ");
//            }
//
//        log.info("스케쥴러 실행 날짜:{}",s.getDueDate());
//        }
//
//    }

    public void updateSubscriptions2() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        List<SubscriptionInfo> expireDateList = subscriptionInfoRepository.findExpireDate(LocalDateTime.parse("2026-02-07 19:45:23", formatter));

        for (SubscriptionInfo s : expireDateList) {
            System.out.println("s = " + s.getDueDate());
                Optional<User> user = userRepository.findById(s.getUser().getId());
                user.ifPresent(u -> {
                    u.setUserPay(UserPay.NORMAL);
                    userRepository.save(u);
                });
                // websocket으로 메세지 전송
//                webSocketNotifier.notifyClients("안녕ㅎㅎ");
            log.info("스케쥴러 실행 날짜:{}",s.getDueDate());
        }

    }
}
