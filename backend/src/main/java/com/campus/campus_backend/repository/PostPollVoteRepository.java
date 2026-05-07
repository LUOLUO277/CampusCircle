package com.campus.campus_backend.repository;

import com.campus.campus_backend.domain.PostPollVote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostPollVoteRepository extends JpaRepository<PostPollVote, Long> {
    Optional<PostPollVote> findByPollIdAndUserId(Long pollId, Long userId);

    long countByPollId(Long pollId);
}

