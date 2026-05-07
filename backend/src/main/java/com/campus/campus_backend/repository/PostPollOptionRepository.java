package com.campus.campus_backend.repository;

import com.campus.campus_backend.domain.PostPollOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostPollOptionRepository extends JpaRepository<PostPollOption, Long> {
    List<PostPollOption> findByPollIdOrderByIdAsc(Long pollId);
}

