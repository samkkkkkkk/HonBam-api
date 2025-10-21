package com.example.HonBam.chatapi.component;

import com.example.HonBam.chatapi.dto.ChatReadEvent;
import com.example.HonBam.chatapi.entity.ChatRoom;
import com.example.HonBam.chatapi.entity.ChatRoomUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatEventBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;

    // 메시지 본문 전송 (방 내부)
    public void sendChatMessage(String roomUuid, Object body) {
        messagingTemplate.convertAndSend("/topic/chat.room." + roomUuid, Map.of(
                "type", "MESSAGE",
                "body", body
        ));
        log.debug("[BROADCAST] chat.room.{} MESSAGE", roomUuid);
    }

    // 메시지 읽음 처리 (방 내부)
    public void sendReadUpdate(String roomUuid, ChatReadEvent event) {
        messagingTemplate.convertAndSend("/topic/chat.room." + roomUuid + ".read", Map.of(
                "type", "READ_UPDATE",
                "body", event
        ));
        log.debug("[BROADCAST] chat.room.{}.read READ_UPDATE", roomUuid);
    }

    // 채팅방 목록 요약 업데이트 (유저별)
    public void sendRoomSummaryUpdate(String userId, String roomUuid, long unReadCount) {
        messagingTemplate.convertAndSend("/topic/chat.summary." + userId, Map.of(
                "type", "ROOM_SUMMARY_UPDATE",
                "body", Map.of("roomUuid", roomUuid, "unReadCount", unReadCount)
        ));
        log.debug("[BROADCAST] chat.summary.{} ROOM_SUMMARY_UPDATE", userId);
    }

    // 참여자 전체 요약 브로드캐스트
    public void broadcastRoomSummaryForParticipants(ChatRoom room, List<ChatRoomUser> participants, Map<String, Long> unreadMap, String excluderUserid) {
        for (ChatRoomUser cru : participants) {
            String userId = cru.getUser().getId();
            if (excluderUserid != null && excluderUserid.equals(userId)) continue;

            long count = unreadMap.getOrDefault(userId, 0L);
            sendRoomSummaryUpdate(userId, room.getRoomUuid(), count);
        }
    }
}
