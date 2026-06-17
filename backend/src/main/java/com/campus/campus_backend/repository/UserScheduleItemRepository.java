package com.campus.campus_backend.repository;

import com.campus.campus_backend.domain.UserScheduleItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserScheduleItemRepository extends JpaRepository<UserScheduleItem, Long> {
    Optional<UserScheduleItem> findByIdAndUserId(Long id, Long userId);

    List<UserScheduleItem> findByUserIdOrderByDeadlineAtAscCreatedAtDesc(Long userId);

    List<UserScheduleItem> findByUserIdAndDeadlineAtLessThanEqualOrderByDeadlineAtAscCreatedAtDesc(Long userId,
            LocalDateTime endTime);
}
