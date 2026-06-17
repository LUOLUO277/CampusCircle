package com.campus.campus_backend.service.info;

import com.campus.campus_backend.domain.AggregatedNotice;
import com.campus.campus_backend.domain.User;
import com.campus.campus_backend.domain.UserNoticeStatus;
import com.campus.campus_backend.domain.UserScheduleItem;
import com.campus.campus_backend.repository.AggregatedNoticeRepository;
import com.campus.campus_backend.repository.UserScheduleItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class DeadlineReminderService {
    private final AggregatedNoticeRepository aggregatedNoticeRepository;
    private final UserScheduleItemRepository userScheduleItemRepository;
    private final NoticeCompletionService noticeCompletionService;

    public DeadlineReminderService(AggregatedNoticeRepository aggregatedNoticeRepository,
            UserScheduleItemRepository userScheduleItemRepository,
            NoticeCompletionService noticeCompletionService) {
        this.aggregatedNoticeRepository = aggregatedNoticeRepository;
        this.userScheduleItemRepository = userScheduleItemRepository;
        this.noticeCompletionService = noticeCompletionService;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getReminders(User user, int days) {
        int safeDays = Math.max(1, Math.min(days, 30));
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime end = now.plusDays(safeDays);
        List<Map<String, Object>> reminders = new ArrayList<>();

        List<AggregatedNotice> notices = user == null
                ? aggregatedNoticeRepository.findPublicDeadlineCandidates("ONLINE", end)
                : aggregatedNoticeRepository.findVisibleDeadlineCandidates("ONLINE", user.getId(), end);
        Map<Long, UserNoticeStatus> statusMap = user == null ? Map.of()
                : noticeCompletionService.getStatusMap(user.getId(), notices.stream().map(AggregatedNotice::getId).toList());
        for (AggregatedNotice notice : notices) {
            UserNoticeStatus status = statusMap.get(notice.getId());
            if (status != null && Boolean.TRUE.equals(status.getCompleted())) {
                continue;
            }
            if (notice.getDeadline() == null) {
                continue;
            }
            reminders.add(toNoticeReminder(notice, now));
        }

        if (user != null) {
            List<UserScheduleItem> scheduleItems = userScheduleItemRepository
                    .findByUserIdAndDeadlineAtLessThanEqualOrderByDeadlineAtAscCreatedAtDesc(user.getId(), end);
            for (UserScheduleItem item : scheduleItems) {
                if (Boolean.TRUE.equals(item.getCompleted()) || item.getDeadlineAt() == null) {
                    continue;
                }
                reminders.add(toScheduleReminder(item, now));
            }
        }

        reminders.sort(Comparator
                .comparing((Map<String, Object> item) -> Boolean.TRUE.equals(item.get("expired")) ? 0 : 1)
                .thenComparing(item -> String.valueOf(item.get("deadlineAt"))));
        return Map.of("list", reminders, "days", safeDays, "generatedAt", now.toString());
    }

    private Map<String, Object> toNoticeReminder(AggregatedNotice notice, LocalDateTime now) {
        Map<String, Object> map = baseReminderMap("NOTICE", notice.getId(), notice.getTitle(), notice.getSummary(),
                notice.getDeadline(), inferType(notice), notice.getSourceName(), now);
        map.put("route", "/pages/info-center/detail?id=" + notice.getId());
        map.put("sourceId", notice.getId());
        return map;
    }

    private Map<String, Object> toScheduleReminder(UserScheduleItem item, LocalDateTime now) {
        Map<String, Object> map = baseReminderMap("MANUAL", item.getId(), item.getTitle(), item.getDescription(),
                item.getDeadlineAt(), item.getType().name(), "手动日程", now);
        map.put("route", "/pages/schedule/form?id=" + item.getId());
        map.put("sourceId", item.getId());
        return map;
    }

    private Map<String, Object> baseReminderMap(String sourceType, Long id, String title, String description,
            LocalDateTime deadlineAt, String type, String sourceName, LocalDateTime now) {
        long hours = Duration.between(now, deadlineAt).toHours();
        long daysLeft = Duration.between(now.toLocalDate().atStartOfDay(), deadlineAt.toLocalDate().atStartOfDay()).toDays();
        boolean expired = deadlineAt.isBefore(now);
        boolean dueSoon = !expired && hours <= 24;
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", sourceType + "-" + id);
        map.put("sourceType", sourceType);
        map.put("title", title);
        map.put("description", description == null ? "" : description);
        map.put("deadlineAt", deadlineAt.toString());
        map.put("type", type);
        map.put("sourceName", sourceName == null ? "" : sourceName);
        map.put("daysLeft", daysLeft);
        map.put("expired", expired);
        map.put("dueSoon", dueSoon);
        map.put("completed", false);
        return map;
    }

    private String inferType(AggregatedNotice notice) {
        String text = ((notice.getCategory() == null ? "" : notice.getCategory()) + " "
                + (notice.getTitle() == null ? "" : notice.getTitle()) + " "
                + (notice.getSummary() == null ? "" : notice.getSummary())).toLowerCase(Locale.ROOT);
        if (text.contains("作业") || text.contains("ddl") || text.contains("提交")) {
            return "ASSIGNMENT";
        }
        if (text.contains("考试") || text.contains("答辩")) {
            return "EXAM";
        }
        if (text.contains("报名") || text.contains("申报")) {
            return "SIGNUP";
        }
        if (text.contains("会议")) {
            return "MEETING";
        }
        if (text.contains("项目")) {
            return "PROJECT";
        }
        return "OTHER";
    }
}
