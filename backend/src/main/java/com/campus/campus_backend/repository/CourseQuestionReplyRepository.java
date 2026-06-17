package com.campus.campus_backend.repository;

import com.campus.campus_backend.domain.CourseQuestionReply;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseQuestionReplyRepository extends JpaRepository<CourseQuestionReply, Long> {
    List<CourseQuestionReply> findByQuestionIdOrderByCreatedAtAsc(Long questionId);

    long countByQuestionId(Long questionId);

    boolean existsByQuestionIdAndUserIdAndContent(Long questionId, Long userId, String content);
}
