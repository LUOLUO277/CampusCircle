package com.campus.campus_backend.repository;

import com.campus.campus_backend.domain.NoticeCommentLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NoticeCommentLikeRepository extends JpaRepository<NoticeCommentLike, Long> {
    boolean existsByCommentIdAndUserId(Long commentId, Long userId);
    long countByCommentId(Long commentId);
    Optional<NoticeCommentLike> findByCommentIdAndUserId(Long commentId, Long userId);
}
