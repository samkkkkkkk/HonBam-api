package com.example.HonBam.paymentsapi.toss.repository;

import com.example.HonBam.paymentsapi.toss.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    Optional<Subscription> findByOrderName(String orderName);
}
