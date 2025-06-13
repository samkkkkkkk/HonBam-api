package com.example.HonBam.chatapi.service;

import com.example.HonBam.chatapi.dto.ChatRoomResponseDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChatNotificationService {
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public ChatNotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * 새로운 채팅방이 생성되었음을 관련 사용자들에게 알립니다.
     * @param newRoomData 생성된 채팅방 정보 (프론트엔드에서 필요한 필드 포함)
     * @param memberUserIds 채팅방 멤버들의 사용자 ID 목록
     */
    public void notifyNewRoomCreated(ChatRoomResponseDTO newRoomData, List<String> memberUserIds) {
        if (newRoomData == null || memberUserIds == null || memberUserIds.isEmpty()) {
            return;
        }

        // 각 멤버에게 사용자별 큐로 메시지 전송
        // 프론트엔드 ChatManage.js에서 구독하는 경로: /user/queue/newRoomNotification
        for (String userId : memberUserIds) {
            // ChatRoomResponseDTO는 프론트엔드가 기대하는 새 채팅방 객체 구조여야 합니다.
            // 예: { id: "room123", name: "새로운 방", lastMessage: "방이 생성되었습니다.", ... }
            messagingTemplate.convertAndSendToUser(
                    userId,                  // 대상 사용자 ID
                    "/queue/newRoomNotification", // 대상 사용자의 구독 경로 (앞의 /user는 Spring이 자동으로 처리)
                    newRoomData              // 전송할 데이터 (JSON으로 직렬화됨)
            );
            System.out.println("Sent new room notification to user: " + userId + " for room: " + newRoomData.getId());
        }
    }
}
