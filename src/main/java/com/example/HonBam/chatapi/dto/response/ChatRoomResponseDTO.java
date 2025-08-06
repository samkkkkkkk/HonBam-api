package com.example.HonBam.chatapi.dto.response;

import com.example.HonBam.chatapi.entity.ChatRoom;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class ChatRoomResponseDTO {
    private String id; // Or Long, ensure consistency with frontend
    private String name;
    private List<String> members;
    private String lastMessage;
    private String timestamp;
    private String avatar;
    private int unreadCount;

    // Constructors
    public ChatRoomResponseDTO(String id, String name, List<String> members, String lastMessage, String timestamp, String avatar, int unreadCount) {
        this.id = id;
        this.name = name;
        this.members = members;
        this.lastMessage = lastMessage;
        this.timestamp = timestamp;
        this.avatar = avatar;
        this.unreadCount = unreadCount;
    }
}
