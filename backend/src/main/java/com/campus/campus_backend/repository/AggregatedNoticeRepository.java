package com.campus.campus_backend.repository;

import com.campus.campus_backend.domain.AggregatedNotice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AggregatedNoticeRepository extends JpaRepository<AggregatedNotice, Long> {
    Optional<AggregatedNotice> findBySourceIdAndExternalId(Long sourceId, String externalId);
    Optional<AggregatedNotice> findBySourceIdAndOwnerUserIdAndExternalId(Long sourceId, Long ownerUserId, String externalId);
    Page<AggregatedNotice> findByStatusOrderByPublishTimeDescCreatedAtDesc(String status, Pageable pageable);
    Page<AggregatedNotice> findBySourceIdInAndStatusOrderByPublishTimeDescCreatedAtDesc(Collection<Long> sourceIds, String status, Pageable pageable);
    List<AggregatedNotice> findTop20ByStatusOrderByPublishTimeDescCreatedAtDesc(String status);

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
}
