package com.example.HonBam.notification.repository;

import com.example.HonBam.notification.entity.Notification;
import com.example.HonBam.notification.entity.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findTop50ByReceiverIdOrderByIdDesc(String receiverId);

    long countByReceiverIdAndReadIsFalse(String receiverId);

    Page<Notification> findByReceiverIdOrderByCreatedAtDesc(String receiverId, Pageable pageable);

    Page<Notification> findByReceiverIdAndIsReadFalseOrderByCreatedAtDesc(String receiverId, Pageable pageable);

    long countByReceiverIdAndIsReadFalse(String receiverId);

    Page<Notification> findByReceiverIdAndNotificationType(
            String receiverId,
            NotificationType type,
            Pageable pageable
    );
}
