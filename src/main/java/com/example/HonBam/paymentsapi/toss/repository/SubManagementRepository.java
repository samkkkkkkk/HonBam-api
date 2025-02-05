package com.example.HonBam.paymentsapi.toss.repository;

import com.example.HonBam.paymentsapi.toss.entity.SubManagement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubManagementRepository extends JpaRepository<SubManagement, Long> {

//    List<SubManagement> findByUserId(String userId);

    @Query("SELECT s FROM SubManagement s JOIN FETCH s.paidInfo p JOIN FETCH s.subscription JOIN FETCH p.user u where u.id = :userId")
    Optional<List<SubManagement>> findByUserIdWithFetchJoin(@Param("userId") String userId);

}
