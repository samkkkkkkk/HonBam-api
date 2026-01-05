package com.example.HonBam.chatapi.service;

import com.example.HonBam.chatapi.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ChatRoomUpdateService {

    private final ChatRoomRepository chatRoomRepository;

    @Transactional
    public void updateLastMessage(Long roomId, String preview, LocalDateTime timestamp, Long messageId) {
        chatRoomRepository.updateLastMessage(roomId, preview, timestamp, messageId);
    }

}
