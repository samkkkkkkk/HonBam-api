package com.example.HonBam.userapi.repository;

import com.example.HonBam.userapi.entity.User;
import com.example.HonBam.userapi.entity.UserProfileMedia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;


public interface UserProfileMediaRepository extends JpaRepository<UserProfileMedia, Long> {

    Optional<UserProfileMedia> findByUser(User user);

    List<UserProfileMedia> findByUser_IdIn(Collection<String> userIds);
}
