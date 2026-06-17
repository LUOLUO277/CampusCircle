package com.campus.campus_backend.service.info;

import com.campus.campus_backend.common.BizException;
import com.campus.campus_backend.common.ErrorCode;
import com.campus.campus_backend.domain.AggregatedNotice;
import com.campus.campus_backend.domain.User;
import com.campus.campus_backend.domain.UserScheduleItem;
import com.campus.campus_backend.dto.info.ScheduleItemRequest;
import com.campus.campus_backend.repository.AggregatedNoticeRepository;
import com.campus.campus_backend.repository.UserScheduleItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ScheduleItemService {
    private final UserScheduleItemRepository userScheduleItemRepository;
    private final AggregatedNoticeRepository aggregatedNoticeRepository;

    public ScheduleItemService(UserScheduleItemRepository userScheduleItemRepository,
            AggregatedNoticeRepository aggregatedNoticeRepository) {
        this.userScheduleItemRepository = userScheduleItemRepository;
        this.aggregatedNoticeRepository = aggregatedNoticeRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> list(User user, String filter) {
        List<Map<String, Object>> list = userScheduleItemRepository.findByUserIdOrderByDeadlineAtAscCreatedAtDesc(user.getId())
                .stream()
                .filter(item -> matchFilter(item, filter))
                .map(this::toMap)
                .toList();
        return Map.of("list", list, "total", list.size());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> detail(Long id, User user) {
        return toMap(requireOwned(id, user));
    }

    @Transactional
    public Map<String, Object> create(User user, ScheduleItemRequest request) {
        UserScheduleItem item = new UserScheduleItem();
        fill(item, user, request);
        userScheduleItemRepository.save(item);
        return toMap(item);
    }

    @Transactional
    public Map<String, Object> update(Long id, User user, ScheduleItemRequest request) {
        UserScheduleItem item = requireOwned(id, user);
        fill(item, user, request);
        userScheduleItemRepository.save(item);
        return toMap(item);
    }

    @Transactional
    public void delete(Long id, User user) {
        userScheduleItemRepository.delete(requireOwned(id, user));
    }

    @Transactional
    public Map<String, Object> complete(Long id, User user) {
        UserScheduleItem item = requireOwned(id, user);
        item.setCompleted(true);
        item.setCompletedAt(LocalDateTime.now());
        userScheduleItemRepository.save(item);
        return toMap(item);
    }

    @Transactional
    public Map<String, Object> uncomplete(Long id, User user) {
        UserScheduleItem item = requireOwned(id, user);
        item.setCompleted(false);
        item.setCompletedAt(null);
        userScheduleItemRepository.save(item);
        return toMap(item);
    }

    private void fill(UserScheduleItem item, User user, ScheduleItemRequest request) {
        item.setUser(user);
        item.setTitle(request.getTitle().trim());
        item.setDescription(request.getDescription());
        item.setDeadlineAt(parseDateTime(request.getDeadlineAt()));
        item.setType(request.getType());
        item.setRelatedNotice(resolveNotice(request.getRelatedNoticeId()));
    }

    private AggregatedNotice resolveNotice(Long relatedNoticeId) {
        if (relatedNoticeId == null) {
            return null;
        }
        return aggregatedNoticeRepository.findById(relatedNoticeId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));
    }

    private UserScheduleItem requireOwned(Long id, User user) {
        return userScheduleItemRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));
    }

    private Map<String, Object> toMap(UserScheduleItem item) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", item.getId());
        map.put("title", item.getTitle());
        map.put("description", item.getDescription());
        map.put("deadlineAt", item.getDeadlineAt() == null ? null : item.getDeadlineAt().toString());
        map.put("type", item.getType().name());
        map.put("completed", Boolean.TRUE.equals(item.getCompleted()));
        map.put("completedAt", item.getCompletedAt() == null ? null : item.getCompletedAt().toString());
        map.put("relatedNoticeId", item.getRelatedNotice() == null ? null : item.getRelatedNotice().getId());
        map.put("createdAt", item.getCreatedAt() == null ? null : item.getCreatedAt().toString());
        map.put("updatedAt", item.getUpdatedAt() == null ? null : item.getUpdatedAt().toString());
        return map;
    }

    private LocalDateTime parseDateTime(String value) {
        try {
            return OffsetDateTime.parse(value).toLocalDateTime();
        } catch (Exception ignore) {
        }
        try {
            return LocalDateTime.parse(value);
        } catch (Exception ignore) {
        }
        try {
            return LocalDateTime.parse(value.replace(" ", "T"));
        } catch (Exception ignore) {
        }
        throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "截止时间格式不正确");
    }

    private boolean matchFilter(UserScheduleItem item, String filter) {
        if (filter == null || filter.isBlank() || "all".equalsIgnoreCase(filter)) {
            return true;
        }
        LocalDateTime now = LocalDateTime.now();
        return switch (filter.toLowerCase()) {
            case "unfinished" -> !Boolean.TRUE.equals(item.getCompleted());
            case "completed" -> Boolean.TRUE.equals(item.getCompleted());
            case "next7days" -> item.getDeadlineAt() != null
                    && !item.getDeadlineAt().isBefore(now)
                    && item.getDeadlineAt().isBefore(now.plusDays(7))
                    && !Boolean.TRUE.equals(item.getCompleted());
            case "expired" -> item.getDeadlineAt() != null
                    && item.getDeadlineAt().isBefore(now)
                    && !Boolean.TRUE.equals(item.getCompleted());
            default -> true;
        };
    }
}
