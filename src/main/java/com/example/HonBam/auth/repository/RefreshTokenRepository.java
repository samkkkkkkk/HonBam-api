package com.example.HonBam.auth.repository;

import com.example.HonBam.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {

    void deleteAllByUserId(String id);


    Optional<RefreshToken> findByTokenHash(String refreshHash);

    List<RefreshToken> findAllByUserId(String userId);

}
