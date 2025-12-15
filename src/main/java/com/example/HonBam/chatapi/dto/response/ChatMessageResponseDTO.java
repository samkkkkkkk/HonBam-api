package com.example.HonBam.chatapi.dto.response;

import com.example.HonBam.chatapi.entity.MessageType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageResponseDTO {
    private Long id;
    private String roomUuid;    // UUID만 노출
    private String senderId;
    private String senderName;
    private MessageType messageType;
    private String content;

    private String fileUrl;
    private String fileName;
    private Long fileSize;

    @Builder.Default
    private List<FileInfoDTO> files = Collections.emptyList();

    private LocalDateTime timestamp;
    private Long unReadUserCount;

    @Getter
    @Builder
    public static class FileInfoDTO {
        private Long mediaId;
        private String fileUrl;
        private String fileName;
        private String contentType;
        private Long fileSize;
    }

}
