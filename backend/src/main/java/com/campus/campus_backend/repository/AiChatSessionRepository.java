package com.campus.campus_backend.repository;

import com.campus.campus_backend.domain.AiChatSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AiChatSessionRepository extends JpaRepository<AiChatSession, Long> {
    List<AiChatSession> findByUserIdOrderByUpdatedAtDesc(Long userId);

    Optional<AiChatSession> findByIdAndUserId(Long id, Long userId);
}
