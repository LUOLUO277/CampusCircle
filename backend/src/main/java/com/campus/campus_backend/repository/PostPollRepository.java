package com.campus.campus_backend.repository;

import com.campus.campus_backend.domain.PostPoll;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostPollRepository extends JpaRepository<PostPoll, Long> {
    Optional<PostPoll> findByPostId(Long postId);
}

