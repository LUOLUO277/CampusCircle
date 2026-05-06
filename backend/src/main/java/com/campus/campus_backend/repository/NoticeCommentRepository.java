package com.campus.campus_backend.repository;

import com.campus.campus_backend.domain.NoticeComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoticeCommentRepository extends JpaRepository<NoticeComment, Long> {
    List<NoticeComment> findByNoticeIdAndParentIdIsNullOrderByCreatedAtAsc(Long noticeId);
    List<NoticeComment> findByParentIdOrderByCreatedAtAsc(Long parentId);
}
