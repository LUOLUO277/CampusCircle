package com.campus.campus_backend.repository;

import com.campus.campus_backend.domain.SubscriptionSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionSourceRepository extends JpaRepository<SubscriptionSource, Long> {
    List<SubscriptionSource> findByStatusOrderByUpdatedAtDesc(String status);
    List<SubscriptionSource> findByNameContainingIgnoreCaseOrSearchKeywordsContainingIgnoreCaseOrderByUpdatedAtDesc(String name, String searchKeywords);
    Optional<SubscriptionSource> findBySourceKey(String sourceKey);
}
