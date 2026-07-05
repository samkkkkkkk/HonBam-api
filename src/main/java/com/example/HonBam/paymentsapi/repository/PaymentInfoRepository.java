package com.example.HonBam.paymentsapi.repository;

import com.example.HonBam.paymentsapi.entity.PaymentInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentInfoRepository extends JpaRepository<PaymentInfo, Long> {

    Optional<PaymentInfo> findByOrderId(String orderId);

}
