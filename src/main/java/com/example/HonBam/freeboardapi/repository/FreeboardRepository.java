package com.example.HonBam.freeboardapi.repository;

import com.example.HonBam.freeboardapi.entity.Freeboard;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FreeboardRepository extends
        JpaRepository<Freeboard, Long> {


}
