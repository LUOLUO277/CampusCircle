package com.campus.campus_backend.repository;

import com.campus.campus_backend.domain.SourceFetchLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface SourceFetchLogRepository extends JpaRepository<SourceFetchLog, Long> {
    List<SourceFetchLog> findTop20BySourceIdOrderByCreatedAtDesc(Long sourceId);

    @Modifying
    @Transactional
    @Query("""
            delete from SourceFetchLog l
            where l.source.id in (
                select s.id from SubscriptionSource s
                where s.fetchStrategy is not null and upper(s.fetchStrategy) like 'MOCK%'
            )
            """)
    int deleteByMockSources();
}
