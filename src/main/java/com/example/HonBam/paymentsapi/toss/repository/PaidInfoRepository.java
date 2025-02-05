package com.example.HonBam.paymentsapi.toss.repository;

import com.example.HonBam.paymentsapi.toss.entity.PaidInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.lang.model.element.Name;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PaidInfoRepository extends JpaRepository<PaidInfo, Long> {


    @Modifying
    @Query(value = "UPDATE paid_info SET (payment_status = :status, paid_at = :requestedAt) WHERE order_id = :orderId", nativeQuery = true)
    int updatePaymentStatus(@Param("status") String status, @Param("orderId") String orderId, @Param("requestedAt") LocalDateTime requestedAt);

    Optional<PaidInfo> findByOrderId(String orderId);

}
