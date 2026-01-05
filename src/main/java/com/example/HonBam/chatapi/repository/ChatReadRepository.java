package com.example.HonBam.chatapi.repository;

import com.example.HonBam.chatapi.entity.ChatRead;
import com.example.HonBam.chatapi.entity.ChatReadId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatReadRepository extends JpaRepository<ChatRead, ChatReadId> {
    boolean existsById(ChatReadId id);
}
