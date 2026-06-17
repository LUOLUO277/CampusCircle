package com.campus.campus_backend.repository;

import com.campus.campus_backend.domain.AiRetrievedSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiRetrievedSourceRepository extends JpaRepository<AiRetrievedSource, Long> {
    List<AiRetrievedSource> findByMessageIdOrderByScoreDescIdAsc(Long messageId);

    void deleteByMessageSessionId(Long sessionId);
}
