package com.campus.campus_backend.repository;

import com.campus.campus_backend.domain.AggregatedNotice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

public interface AggregatedNoticeRepository extends JpaRepository<AggregatedNotice, Long> {
    Optional<AggregatedNotice> findBySourceIdAndExternalId(Long sourceId, String externalId);
    Optional<AggregatedNotice> findBySourceIdAndOwnerUserIdAndExternalId(Long sourceId, Long ownerUserId, String externalId);
    Page<AggregatedNotice> findByStatusOrderByPublishTimeDescCreatedAtDesc(String status, Pageable pageable);
    Page<AggregatedNotice> findBySourceIdInAndStatusOrderByPublishTimeDescCreatedAtDesc(Collection<Long> sourceIds, String status, Pageable pageable);
    List<AggregatedNotice> findTop20ByStatusOrderByPublishTimeDescCreatedAtDesc(String status);
    List<AggregatedNotice> findTop20ByStatusAndCategoryIsNullOrderByPublishTimeDescCreatedAtDesc(String status);

    @Query("""
            select n from AggregatedNotice n
            where n.status = :status
              and (n.publishTime is null or n.publishTime >= :startTime)
            order by n.publishTime desc, n.createdAt desc
            """)
    List<AggregatedNotice> findRecentForAi(@Param("status") String status,
                                           @Param("startTime") LocalDateTime startTime,
                                           Pageable pageable);

    @Query("""
            select n from AggregatedNotice n
            where n.status = :status and (n.category is null or trim(n.category) = '')
            order by n.publishTime desc, n.createdAt desc
            """)
    List<AggregatedNotice> findTopNForAiClassify(@Param("status") String status, Pageable pageable);

    default List<AggregatedNotice> findTopNForAiClassify(String status, int limit) {
        return findTopNForAiClassify(status, Pageable.ofSize(limit));
    }

    @Query("""
            select n from AggregatedNotice n
            where n.status = :status
            order by n.publishTime desc, n.createdAt desc
            """)
    List<AggregatedNotice> findTopNByStatusOrderByPublishTimeDescCreatedAtDesc(@Param("status") String status, Pageable pageable);

    default List<AggregatedNotice> findTopNByStatusOrderByPublishTimeDescCreatedAtDesc(String status, int limit) {
        return findTopNByStatusOrderByPublishTimeDescCreatedAtDesc(status, Pageable.ofSize(limit));
    }

    @Query("""
            select n from AggregatedNotice n
            where n.status = :status and n.ownerUser is null
            order by n.publishTime desc, n.createdAt desc
            """)
    Page<AggregatedNotice> findPublicNotices(@Param("status") String status, Pageable pageable);

    @Query("""
            select n from AggregatedNotice n
            where n.status = :status and (n.ownerUser is null or n.ownerUser.id = :userId)
            order by n.publishTime desc, n.createdAt desc
            """)
    Page<AggregatedNotice> findVisibleNotices(@Param("status") String status, @Param("userId") Long userId, Pageable pageable);

    @Query("""
            select n from AggregatedNotice n
            where n.status = :status
              and ((n.ownerUser is null and n.source.id in :sourceIds) or n.ownerUser.id = :userId)
            order by n.publishTime desc, n.createdAt desc
            """)
    Page<AggregatedNotice> findSubscribedAndOwnedVisibleNotices(@Param("status") String status,
            @Param("sourceIds") Collection<Long> sourceIds,
            @Param("userId") Long userId,
            Pageable pageable);

    @Modifying
    @Transactional
    @Query("""
            delete from AggregatedNotice n
            where n.source.id in (
                select s.id from SubscriptionSource s
                where s.fetchStrategy is not null and upper(s.fetchStrategy) like 'MOCK%'
            )
            """)
    int deleteByMockSources();

    @Modifying
    @Transactional
    @Query("""
            delete from AggregatedNotice n
            where n.rawPayloadJson is not null and n.rawPayloadJson like %:needle%
            """)
    int deleteByRawPayloadLike(@Param("needle") String needle);
}
