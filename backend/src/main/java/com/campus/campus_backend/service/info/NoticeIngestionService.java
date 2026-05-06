package com.campus.campus_backend.service.info;

import com.campus.campus_backend.domain.AggregatedNotice;
import com.campus.campus_backend.domain.SourceFetchLog;
import com.campus.campus_backend.domain.SubscriptionSource;
import com.campus.campus_backend.repository.AggregatedNoticeRepository;
import com.campus.campus_backend.repository.SourceFetchLogRepository;
import com.campus.campus_backend.repository.SubscriptionSourceRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class NoticeIngestionService {
    private static final Pattern DATETIME_PATTERN = Pattern.compile("(20\\d{2}-\\d{2}-\\d{2})(?:\\s+(\\d{2}:\\d{2}))?");
    private static final Pattern URL_PATTERN = Pattern.compile("(https?://\\S+)");
    private final AggregatedNoticeRepository aggregatedNoticeRepository;
    private final SubscriptionSourceRepository subscriptionSourceRepository;
    private final SourceFetchLogRepository sourceFetchLogRepository;
    private final ObjectMapper objectMapper;

    public NoticeIngestionService(AggregatedNoticeRepository aggregatedNoticeRepository,
            SubscriptionSourceRepository subscriptionSourceRepository,
            SourceFetchLogRepository sourceFetchLogRepository,
            ObjectMapper objectMapper) {
        this.aggregatedNoticeRepository = aggregatedNoticeRepository;
        this.subscriptionSourceRepository = subscriptionSourceRepository;
        this.sourceFetchLogRepository = sourceFetchLogRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Map<String, Object> ingest(SubscriptionSource source, List<RawNoticeItem> items) {
        return ingest(source, null, items);
    }

    @Transactional
    public Map<String, Object> ingest(SubscriptionSource source, com.campus.campus_backend.domain.User ownerUser, List<RawNoticeItem> items) {
        int successCount = 0;
        int failureCount = 0;
        for (RawNoticeItem item : items) {
            try {
                AggregatedNotice notice = ownerUser == null
                        ? aggregatedNoticeRepository.findBySourceIdAndExternalId(source.getId(), item.getExternalId())
                                .orElseGet(AggregatedNotice::new)
                        : aggregatedNoticeRepository.findBySourceIdAndOwnerUserIdAndExternalId(source.getId(),
                                ownerUser.getId(), item.getExternalId()).orElseGet(AggregatedNotice::new);
                notice.setSource(source);
                notice.setOwnerUser(ownerUser);
                notice.setExternalId(item.getExternalId());
                notice.setTitle(item.getTitle());
                notice.setCategory(defaultIfBlank(item.getCategory(), inferCategory(item.getContent(), item.getTitle())));
                notice.setSourceName(source.getName());
                notice.setOriginalUrl(item.getOriginalUrl());
                notice.setPublishTime(item.getPublishTime() != null ? item.getPublishTime() : LocalDateTime.now());
                notice.setContentSnapshot(item.getContent());
                notice.setSummary(buildSummary(item.getContent(), item.getTitle()));
                notice.setDeadline(extractDeadline(item.getContent()));
                notice.setTargetAudience(extractAudience(item.getContent()));
                notice.setLocation(extractLocation(item.getContent()));
                notice.setActionLinksJson(writeJson(extractLinks(item.getContent(), item.getOriginalUrl())));
                notice.setTagsJson(writeJson(extractTags(item)));
                notice.setRawPayloadJson(writeJson(item.getRawPayload()));
                notice.setExtractionStatus("DONE");
                notice.setStatus("ONLINE");
                aggregatedNoticeRepository.save(notice);
                successCount++;
            } catch (Exception ex) {
                failureCount++;
            }
        }

        source.setLastFetchedAt(LocalDateTime.now());
        source.setLastFetchStatus(failureCount > 0 ? "PARTIAL_SUCCESS" : "SUCCESS");
        subscriptionSourceRepository.save(source);

        SourceFetchLog log = new SourceFetchLog();
        log.setSource(source);
        log.setSuccessCount(successCount);
        log.setFailureCount(failureCount);
        log.setStatus(failureCount > 0 ? "PARTIAL_SUCCESS" : "SUCCESS");
        sourceFetchLogRepository.save(log);

        return Map.of("successCount", successCount, "failureCount", failureCount);
    }

    public AggregatedNotice saveManualNotice(SubscriptionSource source, String externalId, String title, String summary,
            String category, String originalUrl, LocalDateTime publishTime, LocalDateTime deadline, String targetAudience,
            String location, String contentSnapshot, List<String> tags, List<Map<String, String>> actionLinks) {
        AggregatedNotice notice = aggregatedNoticeRepository.findBySourceIdAndExternalId(source.getId(), externalId)
                .orElseGet(AggregatedNotice::new);
        notice.setSource(source);
        notice.setExternalId(externalId);
        notice.setTitle(title);
        notice.setSummary(defaultIfBlank(summary, buildSummary(contentSnapshot, title)));
        notice.setCategory(defaultIfBlank(category, inferCategory(contentSnapshot, title)));
        notice.setSourceName(source.getName());
        notice.setOriginalUrl(originalUrl);
        notice.setPublishTime(publishTime != null ? publishTime : LocalDateTime.now());
        notice.setDeadline(deadline);
        notice.setTargetAudience(targetAudience);
        notice.setLocation(location);
        notice.setActionLinksJson(writeJson(actionLinks));
        notice.setTagsJson(writeJson(tags));
        notice.setContentSnapshot(contentSnapshot);
        notice.setRawPayloadJson(writeJson(Map.of("manual", true)));
        notice.setExtractionStatus("MANUAL");
        notice.setStatus("ONLINE");
        return aggregatedNoticeRepository.save(notice);
    }

    private String buildSummary(String content, String title) {
        String text = defaultIfBlank(content, title);
        if (text == null) {
            return null;
        }
        text = text.replaceAll("\\s+", " ").trim();
        return text.length() > 120 ? text.substring(0, 120) + "..." : text;
    }

    private LocalDateTime extractDeadline(String text) {
        if (text == null) {
            return null;
        }
        Matcher matcher = DATETIME_PATTERN.matcher(text);
        while (matcher.find()) {
            String datePart = matcher.group(1);
            String timePart = matcher.group(2) != null ? matcher.group(2) : "23:59";
            LocalDateTime parsed = LocalDateTime.parse(datePart + " " + timePart,
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            if (text.contains("截止") || text.contains("前完成") || text.contains("前提交")) {
                return parsed;
            }
        }
        return null;
    }

    private String extractAudience(String text) {
        if (text == null) {
            return null;
        }
        for (String marker : List.of("适用对象：", "适用对象:", "面向", "对象：", "对象:")) {
            int index = text.indexOf(marker);
            if (index >= 0) {
                String candidate = text.substring(index + marker.length()).trim();
                int end = candidate.indexOf('。');
                if (end > 0) {
                    candidate = candidate.substring(0, end);
                }
                return candidate.length() > 80 ? candidate.substring(0, 80) : candidate;
            }
        }
        return null;
    }

    private String extractLocation(String text) {
        if (text == null) {
            return null;
        }
        for (String marker : List.of("地点", "地点：", "地点:", "地址：", "地址:")) {
            int index = text.indexOf(marker);
            if (index >= 0) {
                String candidate = text.substring(index + marker.length()).trim();
                int end = candidate.indexOf('，');
                if (end < 0) {
                    end = candidate.indexOf('。');
                }
                if (end > 0) {
                    candidate = candidate.substring(0, end);
                }
                return candidate.length() > 80 ? candidate.substring(0, 80) : candidate;
            }
        }
        return null;
    }

    private List<Map<String, String>> extractLinks(String text, String originalUrl) {
        List<Map<String, String>> links = new ArrayList<>();
        if (originalUrl != null && !originalUrl.isBlank()) {
            links.add(Map.of("label", "原文", "url", originalUrl));
        }
        if (text != null) {
            Matcher matcher = URL_PATTERN.matcher(text);
            while (matcher.find()) {
                String url = matcher.group(1);
                links.add(Map.of("label", "相关链接", "url", url));
            }
        }
        return links;
    }

    private List<String> extractTags(RawNoticeItem item) {
        Set<String> tags = new LinkedHashSet<>();
        if (item.getTags() != null) {
            tags.addAll(item.getTags());
        }
        String text = (defaultIfBlank(item.getTitle(), "") + " " + defaultIfBlank(item.getContent(), "")).toLowerCase();
        if (text.contains("报名")) {
            tags.add("报名");
        }
        if (text.contains("截止")) {
            tags.add("截止");
        }
        if (text.contains("活动")) {
            tags.add("活动");
        }
        return new ArrayList<>(tags);
    }

    private String inferCategory(String content, String title) {
        String text = (defaultIfBlank(title, "") + " " + defaultIfBlank(content, "")).toLowerCase();
        if (text.contains("截止") || text.contains("ddl")) {
            return "截止提醒";
        }
        if (text.contains("活动")) {
            return "活动";
        }
        if (text.contains("报名")) {
            return "报名";
        }
        return "通知";
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
