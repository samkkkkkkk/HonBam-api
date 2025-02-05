package com.example.HonBam.paymentsapi.toss.repository;

import com.example.HonBam.paymentsapi.toss.entity.PaymentInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentInfoRepository extends JpaRepository<PaymentInfo, Long> {

    Optional<PaymentInfo> findByOrderId(String orderId);

    @Query("SELECT p FROM PaymentInfo p WHERE p.orderId = :orderId")
    PaymentInfo findPaymentByOrderId(@Param("orderId") String orderId);

}
