package com.example.HonBam.upload.repository;

import com.example.HonBam.upload.entity.Media;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MediaRepository extends JpaRepository<Media, Long> {

    Optional<Media> findByFileKey(String fileKey);

    boolean existsByFileKey(String fileKey);
}
