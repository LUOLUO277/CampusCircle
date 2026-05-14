package com.campus.campus_backend.repository;

import com.campus.campus_backend.domain.NoticeSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface NoticeSubscriptionRepository extends JpaRepository<NoticeSubscription, Long> {
    List<NoticeSubscription> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<NoticeSubscription> findByUserIdAndEnabledTrue(Long userId);
    Optional<NoticeSubscription> findByUserIdAndSourceId(Long userId, Long sourceId);
    boolean existsByUserIdAndSourceIdAndEnabledTrue(Long userId, Long sourceId);

    @Modifying
    @Transactional
    @Query("""
            delete from NoticeSubscription n
            where n.source.id in (
                select s.id from SubscriptionSource s
                where s.fetchStrategy is not null and upper(s.fetchStrategy) like 'MOCK%'
            )
            """)
    int deleteByMockSources();
}
