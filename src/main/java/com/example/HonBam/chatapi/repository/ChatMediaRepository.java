package com.example.HonBam.chatapi.repository;

import com.example.HonBam.chatapi.entity.ChatMedia;
import com.example.HonBam.chatapi.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMediaRepository extends JpaRepository<ChatMedia, Long> {

    // 관련된 미디어들 한 번에 가져오기
    List<ChatMedia> findByMessageIn(List<ChatMessage> messages);

    // 단일 메시지에 대한 미디어 조회
    List<ChatMedia> findByMessageId(Long messageId);

    // 메시지 삭제 시 미디어 데이터 삭제
    void deleteByMessage(ChatMessage message);

    @Query("select cm from ChatMedia cm " +
           "join fetch cm.media m " +
           "where cm.message.id = :messageId")
    List<ChatMedia> findByMessageIdWithMedia(@Param("messageId") Long messageId);
}
