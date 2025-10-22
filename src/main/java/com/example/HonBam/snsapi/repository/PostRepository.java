package com.example.HonBam.snsapi.repository;

import com.example.HonBam.snsapi.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {
    Page<Post> findByAuthorIdInOrderByIdDesc(Iterable<String> followingIds, Pageable pageable);

}
