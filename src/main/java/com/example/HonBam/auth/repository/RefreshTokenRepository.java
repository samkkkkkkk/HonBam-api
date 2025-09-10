package com.example.HonBam.auth.repository;

import com.example.HonBam.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {

    void deleteAllByUserId(String id);
}
