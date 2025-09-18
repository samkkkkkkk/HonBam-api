//package com.example.HonBam.chatapi.component;
//
//// WebSocketConfig.java 또는 별도의 이벤트 리스너 클래스
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.ApplicationListener;
//import org.springframework.data.redis.core.StringRedisTemplate;
//import org.springframework.messaging.simp.SimpMessagingTemplate;
//import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
//import org.springframework.stereotype.Component;
//import org.springframework.web.socket.messaging.SessionConnectedEvent;
//import org.springframework.web.socket.messaging.SessionDisconnectEvent;
//import java.security.Principal;
//import java.util.Set;
//
//@Component
//class WebSocketEventListener {
//
//    @Autowired
//    private SimpMessagingTemplate messagingTemplate;
//
//    @Autowired
//    private StringRedisTemplate redisTemplate; // 또는 RedisTemplate<String, Object>
//
//    private static final String ONLINE_USERS_KEY = "online_users";
//    private static final String ONLINE_USERS_TOPIC = "/topic/onlineUsers"; // 전체 목록 브로드캐스트용
//    private static final String USER_STATUS_TOPIC_PREFIX = "/topic/userStatus/"; // 개별 상태 변경 알림용
//
//
//    @Component
//    public class SessionConnectedEventListener implements ApplicationListener<SessionConnectedEvent> {
//        @Override
//        public void onApplicationEvent(SessionConnectedEvent event) {
//            StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
//            Principal userPrincipal = accessor.getUser(); // Spring Security 등으로 인증된 사용자 정보
//
//            if (userPrincipal != null && userPrincipal.getName() != null) {
//                String userId = userPrincipal.getName();
//                redisTemplate.opsForSet().add(ONLINE_USERS_KEY, userId);
//                System.out.println("User connected: " + userId);
//
//                // 옵션 1: 특정 사용자 접속 알림
//                // messagingTemplate.convertAndSend(USER_STATUS_TOPIC_PREFIX + userId, "{\"status\":\"online\"}");
//
//                // 옵션 2: 전체 온라인 사용자 목록 브로드캐스트
//                broadcastOnlineUsers();
//            }
//        }
//    }
//
//    @Component
//    public class SessionDisconnectEventListener implements ApplicationListener<SessionDisconnectEvent> {
//        @Override
//        public void onApplicationEvent(SessionDisconnectEvent event) {
//            StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
//            Principal userPrincipal = accessor.getUser(); // Spring Security 등으로 인증된 사용자 정보
//
//            // Disconnect 시에는 userPrincipal이 null일 수 있으므로, 세션 속성 등 다른 방법으로 사용자 ID를 가져와야 할 수 있음
//            // 여기서는 예시로 userPrincipal을 사용
//            if (userPrincipal != null && userPrincipal.getName() != null) {
//                String userId = userPrincipal.getName();
//                redisTemplate.opsForSet().remove(ONLINE_USERS_KEY, userId);
//                System.out.println("User disconnected: " + userId);
//
//                // 옵션 1: 특정 사용자 접속 종료 알림
//                // messagingTemplate.convertAndSend(USER_STATUS_TOPIC_PREFIX + userId, "{\"status\":\"offline\"}");
//
//                // 옵션 2: 전체 온라인 사용자 목록 브로드캐스트
//                broadcastOnlineUsers();
//            } else {
//                // 연결 시 저장했던 세션 ID와 사용자 ID 매핑 정보를 활용하여 Redis에서 제거
//                String sessionId = accessor.getSessionId();
//                // TODO: 세션 ID를 기반으로 사용자 ID를 찾아 Redis에서 제거하는 로직 필요
//                // (예: 연결 시 redis에 session_id -> user_id 맵 저장)
//                System.out.println("User disconnected with session ID: " + sessionId);
//                // 이 경우, 어떤 유저가 나갔는지 알 수 없으므로 전체 목록을 다시 보내는 것이 안전할 수 있음
//                broadcastOnlineUsers();
//            }
//        }
//    }
//
//    private void broadcastOnlineUsers() {
//        Set<String> onlineUsers = redisTemplate.opsForSet().members(ONLINE_USERS_KEY);
//        messagingTemplate.convertAndSend(ONLINE_USERS_TOPIC, onlineUsers);
//        System.out.println("Broadcasting online users: " + onlineUsers);
//    }
//}
