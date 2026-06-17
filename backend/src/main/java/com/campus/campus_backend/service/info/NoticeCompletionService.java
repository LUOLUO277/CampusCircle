package com.campus.campus_backend.service.info;

import com.campus.campus_backend.common.BizException;
import com.campus.campus_backend.common.ErrorCode;
import com.campus.campus_backend.domain.AggregatedNotice;
import com.campus.campus_backend.domain.User;
import com.campus.campus_backend.domain.UserNoticeStatus;
import com.campus.campus_backend.repository.AggregatedNoticeRepository;
import com.campus.campus_backend.repository.UserNoticeStatusRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class NoticeCompletionService {
    private final UserNoticeStatusRepository userNoticeStatusRepository;
    private final AggregatedNoticeRepository aggregatedNoticeRepository;

    public NoticeCompletionService(UserNoticeStatusRepository userNoticeStatusRepository,
            AggregatedNoticeRepository aggregatedNoticeRepository) {
        this.userNoticeStatusRepository = userNoticeStatusRepository;
        this.aggregatedNoticeRepository = aggregatedNoticeRepository;
    }

    @Transactional
    public Map<String, Object> markCompleted(Long noticeId, User user) {
        AggregatedNotice notice = aggregatedNoticeRepository.findById(noticeId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));
        UserNoticeStatus status = userNoticeStatusRepository.findByUserIdAndNoticeId(user.getId(), noticeId)
                .orElseGet(UserNoticeStatus::new);
        status.setUser(user);
        status.setNotice(notice);
        status.setCompleted(true);
        status.setCompletedAt(LocalDateTime.now());
        userNoticeStatusRepository.save(status);
        return toStatusMap(status);
    }

    @Transactional
    public Map<String, Object> unmarkCompleted(Long noticeId, User user) {
        AggregatedNotice notice = aggregatedNoticeRepository.findById(noticeId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));
        UserNoticeStatus status = userNoticeStatusRepository.findByUserIdAndNoticeId(user.getId(), noticeId)
                .orElseGet(UserNoticeStatus::new);
        status.setUser(user);
        status.setNotice(notice);
        status.setCompleted(false);
        status.setCompletedAt(null);
        userNoticeStatusRepository.save(status);
        return toStatusMap(status);
    }

    @Transactional(readOnly = true)
    public Map<Long, UserNoticeStatus> getStatusMap(Long userId, Collection<Long> noticeIds) {
        if (userId == null || noticeIds == null || noticeIds.isEmpty()) {
            return Map.of();
        }
        return userNoticeStatusRepository.findByUserIdAndNoticeIdIn(userId, noticeIds).stream()
                .collect(Collectors.toMap(status -> status.getNotice().getId(), Function.identity()));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getStatus(Long noticeId, User user) {
        UserNoticeStatus status = userNoticeStatusRepository.findByUserIdAndNoticeId(user.getId(), noticeId).orElse(null);
        if (status == null) {
            return Map.of(
                    "noticeId", noticeId,
                    "completed", false,
                    "completedAt", null);
        }
        return toStatusMap(status);
    }

    private Map<String, Object> toStatusMap(UserNoticeStatus status) {
        return Map.of(
                "noticeId", status.getNotice().getId(),
                "completed", Boolean.TRUE.equals(status.getCompleted()),
                "completedAt", status.getCompletedAt() == null ? null : status.getCompletedAt().toString());
    }
}
