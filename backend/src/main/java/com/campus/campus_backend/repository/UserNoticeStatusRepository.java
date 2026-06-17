package com.campus.campus_backend.repository;

import com.campus.campus_backend.domain.UserNoticeStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserNoticeStatusRepository extends JpaRepository<UserNoticeStatus, Long> {
    Optional<UserNoticeStatus> findByUserIdAndNoticeId(Long userId, Long noticeId);

    List<UserNoticeStatus> findByUserIdAndNoticeIdIn(Long userId, Collection<Long> noticeIds);
}
