package com.campus.campus_backend.repository;

import com.campus.campus_backend.domain.SourceFetchLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SourceFetchLogRepository extends JpaRepository<SourceFetchLog, Long> {
    List<SourceFetchLog> findTop20BySourceIdOrderByCreatedAtDesc(Long sourceId);
}
