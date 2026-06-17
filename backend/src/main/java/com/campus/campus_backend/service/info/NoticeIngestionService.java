package com.campus.campus_backend.service.info;

import com.campus.campus_backend.domain.AggregatedNotice;
import com.campus.campus_backend.domain.SourceFetchLog;
import com.campus.campus_backend.domain.SubscriptionSource;
import com.campus.campus_backend.dto.info.DeadlineExtractionResult;
import com.campus.campus_backend.repository.AggregatedNoticeRepository;
import com.campus.campus_backend.repository.SourceFetchLogRepository;
import com.campus.campus_backend.repository.SubscriptionSourceRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class NoticeIngestionService {
    private static final Pattern URL_PATTERN = Pattern.compile("(https?://\\S+)");
    public static final int MAX_SOURCE_CONFIG_LEN = 20000;
    public static final int MAX_SOURCE_KEYWORDS_LEN = 500;
    public static final int MAX_SOURCE_URL_LEN = 500;
    public static final int MAX_SOURCE_NAME_LEN = 100;
    public static final int MAX_SOURCE_TYPE_LEN = 20;
    public static final int MAX_SOURCE_FETCH_STRATEGY_LEN = 50;
    public static final int MAX_SOURCE_STATUS_LEN = 20;
    private static final int MAX_CONTENT_SNAPSHOT_LEN = 20000;
    private static final int MAX_JSON_FIELD_LEN = 50000;

    private final AggregatedNoticeRepository aggregatedNoticeRepository;
    private final SubscriptionSourceRepository subscriptionSourceRepository;
    private final SourceFetchLogRepository sourceFetchLogRepository;
    private final ObjectMapper objectMapper;
    private final DeadlineExtractionService deadlineExtractionService;

    public NoticeIngestionService(AggregatedNoticeRepository aggregatedNoticeRepository,
            SubscriptionSourceRepository subscriptionSourceRepository,
            SourceFetchLogRepository sourceFetchLogRepository,
            ObjectMapper objectMapper,
            DeadlineExtractionService deadlineExtractionService) {
        this.aggregatedNoticeRepository = aggregatedNoticeRepository;
        this.subscriptionSourceRepository = subscriptionSourceRepository;
        this.sourceFetchLogRepository = sourceFetchLogRepository;
        this.objectMapper = objectMapper;
        this.deadlineExtractionService = deadlineExtractionService;
    }

    @Transactional
    public Map<String, Object> ingest(SubscriptionSource source, List<RawNoticeItem> items) {
        return ingest(source, null, items);
    }

    @Transactional
    public Map<String, Object> ingest(SubscriptionSource source, com.campus.campus_backend.domain.User ownerUser,
            List<RawNoticeItem> items) {
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
                notice.setTitle(truncate(item.getTitle(), 255));
                notice.setCategory(truncate(defaultIfBlank(item.getCategory(), inferCategory(item.getContent(), item.getTitle())), 50));
                notice.setSourceName(source.getName());
                notice.setOriginalUrl(truncate(item.getOriginalUrl(), 500));
                notice.setPublishTime(item.getPublishTime() != null ? item.getPublishTime() : LocalDateTime.now());
                String sanitizedContent = truncate(item.getContent(), MAX_CONTENT_SNAPSHOT_LEN);
                notice.setContentSnapshot(sanitizedContent);
                notice.setSummary(truncate(buildSummary(item.getContent(), item.getTitle()), 500));
                applyExtractedDeadline(notice, item.getTitle(), notice.getSummary(), sanitizedContent, null);
                notice.setTargetAudience(truncate(extractAudience(sanitizedContent), 255));
                notice.setLocation(truncate(extractLocation(sanitizedContent), 255));
                notice.setActionLinksJson(writeJson(extractLinks(sanitizedContent, item.getOriginalUrl())));
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
        normalizeSourceForStorage(source);
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
        String sanitizedContent = truncate(contentSnapshot, MAX_CONTENT_SNAPSHOT_LEN);
        notice.setSource(source);
        notice.setExternalId(externalId);
        notice.setTitle(truncate(title, 255));
        notice.setSummary(truncate(defaultIfBlank(summary, buildSummary(sanitizedContent, title)), 500));
        notice.setCategory(truncate(defaultIfBlank(category, inferCategory(sanitizedContent, title)), 50));
        notice.setSourceName(source.getName());
        notice.setOriginalUrl(truncate(originalUrl, 500));
        notice.setPublishTime(publishTime != null ? publishTime : LocalDateTime.now());
        notice.setTargetAudience(truncate(targetAudience, 255));
        notice.setLocation(truncate(location, 255));
        notice.setActionLinksJson(writeJson(actionLinks));
        notice.setTagsJson(writeJson(tags));
        notice.setContentSnapshot(sanitizedContent);
        notice.setRawPayloadJson(writeJson(Map.of("manual", true)));
        applyExtractedDeadline(notice, title, notice.getSummary(), sanitizedContent, deadline);
        notice.setExtractionStatus(deadline != null ? "MANUAL" : "DONE");
        notice.setStatus("ONLINE");
        return aggregatedNoticeRepository.save(notice);
    }

    public AggregatedNotice rebuildDeadline(AggregatedNotice notice) {
        applyExtractedDeadline(notice, notice.getTitle(), notice.getSummary(), notice.getContentSnapshot(), null);
        return aggregatedNoticeRepository.save(notice);
    }

    public void normalizeSourceForStorage(SubscriptionSource source) {
        if (source == null) {
            return;
        }
        source.setName(truncate(source.getName(), MAX_SOURCE_NAME_LEN));
        source.setType(truncate(source.getType(), MAX_SOURCE_TYPE_LEN));
        source.setSourceUrl(truncate(source.getSourceUrl(), MAX_SOURCE_URL_LEN));
        source.setSearchKeywords(truncate(source.getSearchKeywords(), MAX_SOURCE_KEYWORDS_LEN));
        source.setFetchStrategy(truncate(source.getFetchStrategy(), MAX_SOURCE_FETCH_STRATEGY_LEN));
        source.setFetchConfigJson(truncate(source.getFetchConfigJson(), MAX_SOURCE_CONFIG_LEN));
        source.setStatus(truncate(source.getStatus(), MAX_SOURCE_STATUS_LEN));
        source.setLastFetchStatus(truncate(source.getLastFetchStatus(), 50));
    }

    private void applyExtractedDeadline(AggregatedNotice notice, String title, String summary, String content,
            LocalDateTime explicitDeadline) {
        if (explicitDeadline != null) {
            notice.setDeadline(explicitDeadline);
            notice.setDeadlineSource("manual:explicit");
            notice.setDeadlineConfidence(1.0D);
            return;
        }
        DeadlineExtractionResult extracted = deadlineExtractionService.extract(title, summary, content);
        notice.setDeadline(extracted.getDeadlineAt());
        notice.setDeadlineSource(extracted.getSourceText());
        notice.setDeadlineConfidence(extracted.getConfidence());
    }

    private String buildSummary(String content, String title) {
        String text = defaultIfBlank(content, title);
        if (text == null) {
            return null;
        }
        text = text.replaceAll("\\s+", " ").trim();
        return text.length() > 120 ? text.substring(0, 120) + "..." : text;
    }

    private String extractAudience(String text) {
        if (text == null) {
            return null;
        }
        for (String marker : List.of("适用对象：", "适用对象:", "面向", "对象：", "对象:")) {
            int index = text.indexOf(marker);
            if (index < 0) {
                continue;
            }
            String candidate = text.substring(index + marker.length()).trim();
            candidate = splitOnPunctuation(candidate);
            return candidate.length() > 80 ? candidate.substring(0, 80) : candidate;
        }
        return null;
    }

    private String extractLocation(String text) {
        if (text == null) {
            return null;
        }
        for (String marker : List.of("地点：", "地点:", "地址：", "地址:", "地点")) {
            int index = text.indexOf(marker);
            if (index < 0) {
                continue;
            }
            String candidate = text.substring(index + marker.length()).trim();
            candidate = splitOnPunctuation(candidate);
            return candidate.length() > 80 ? candidate.substring(0, 80) : candidate;
        }
        return null;
    }

    private String splitOnPunctuation(String text) {
        int end = text.length();
        for (String separator : List.of("。", "；", ";", "\n", "，", ",")) {
            int index = text.indexOf(separator);
            if (index >= 0) {
                end = Math.min(end, index);
            }
        }
        return text.substring(0, end).trim();
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
        String text = (defaultIfBlank(item.getTitle(), "") + " " + defaultIfBlank(item.getContent(), "")).toLowerCase(Locale.ROOT);
        if (text.contains("报名")) {
            tags.add("报名");
        }
        if (text.contains("截止") || text.contains("ddl")) {
            tags.add("截止");
        }
        if (text.contains("考试")) {
            tags.add("考试");
        }
        if (text.contains("项目")) {
            tags.add("项目");
        }
        return new ArrayList<>(tags);
    }

    private String inferCategory(String content, String title) {
        String text = (defaultIfBlank(title, "") + " " + defaultIfBlank(content, "")).toLowerCase(Locale.ROOT);
        if (text.contains("截止") || text.contains("ddl")) {
            return "截止提醒";
        }
        if (text.contains("报名")) {
            return "报名";
        }
        if (text.contains("考试")) {
            return "考试";
        }
        if (text.contains("活动")) {
            return "活动";
        }
        return "通知";
    }

    private String writeJson(Object value) {
        try {
            return truncate(objectMapper.writeValueAsString(value), MAX_JSON_FIELD_LEN);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private String truncate(String value, int maxLen) {
        if (value == null || value.length() <= maxLen) {
            return value;
        }
        return value.substring(0, maxLen);
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
