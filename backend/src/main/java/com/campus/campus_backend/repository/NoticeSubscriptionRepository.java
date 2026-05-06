package com.campus.campus_backend.repository;

import com.campus.campus_backend.domain.NoticeSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NoticeSubscriptionRepository extends JpaRepository<NoticeSubscription, Long> {
    List<NoticeSubscription> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<NoticeSubscription> findByUserIdAndEnabledTrue(Long userId);
    Optional<NoticeSubscription> findByUserIdAndSourceId(Long userId, Long sourceId);
    boolean existsByUserIdAndSourceIdAndEnabledTrue(Long userId, Long sourceId);
}
