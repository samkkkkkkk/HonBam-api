package com.example.HonBam.notification.repository;

import com.example.HonBam.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findTop50ByReceiverIdOrderByIdDesc(String receiverId);

    long countByReceiverIdAndReadIsFalse(String receiverId);
    
}
