package com.example.HonBam.userapi.repository;

import com.example.HonBam.userapi.entity.LoginProvider;
import com.example.HonBam.userapi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.persistence.LockModeType;
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
    boolean existsByNickname(String value);

    // 특정 ID를 제외한 모든 사용자 조회
    List<User> findAllByIdNot(String id);

    // 특정 ID의 유저를 조회하면서 비관적 락 걸기
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> findByIdWithLock(@Param("id") String id);

}








