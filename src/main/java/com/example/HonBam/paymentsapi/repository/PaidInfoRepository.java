package com.example.HonBam.paymentsapi.repository;

import com.example.HonBam.paymentsapi.entity.PaidInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaidInfoRepository extends JpaRepository<PaidInfo, Long> {

    Optional<PaidInfo> findByOrderId(String orderId);

}
