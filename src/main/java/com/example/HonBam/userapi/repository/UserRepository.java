package com.example.HonBam.userapi.repository;

import com.example.HonBam.userapi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository
        extends JpaRepository<User, String> {

    // 이메일로 회원 정보 조회
    Optional<User> findByEmail(String email); // 쿼리 메서드

    // 이메일 중복 체크
    // @Query("SELECT COUNT(*) FROM User u WHERE u.email =: email") // JPQL
    boolean existsByEmail(String value);

    // 아이디 중복체크
    boolean existsByUserId(String value);

    // 특정 ID를 제외한 모든 사용자 조회
    List<User> findAllByIdNot(String id);

}








