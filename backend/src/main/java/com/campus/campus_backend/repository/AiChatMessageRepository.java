package com.campus.campus_backend.repository;

import com.campus.campus_backend.domain.AiChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiChatMessageRepository extends JpaRepository<AiChatMessage, Long> {
    List<AiChatMessage> findBySessionIdOrderByCreatedAtAsc(Long sessionId);

    List<AiChatMessage> findTop12BySessionIdOrderByCreatedAtDesc(Long sessionId);

    void deleteBySessionId(Long sessionId);
}
