package com.example.HonBam.paymentsapi.repository;

import com.example.HonBam.paymentsapi.entity.SubManagement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubManagementRepository extends JpaRepository<SubManagement, Long> {

    @Query("SELECT s FROM SubManagement s JOIN FETCH s.paidInfo p JOIN FETCH s.subscription JOIN FETCH p.user u where u.id = :userId")
    List<SubManagement> findByUserIdWithFetchJoin(@Param("userId") String userId);

}
