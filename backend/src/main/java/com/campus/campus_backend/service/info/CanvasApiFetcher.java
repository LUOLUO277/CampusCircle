package com.campus.campus_backend.service.info;

import com.campus.campus_backend.domain.SubscriptionSource;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class CanvasApiFetcher implements NoticeFetcher {
    private final RemoteNoticeSupport remoteNoticeSupport;

    public CanvasApiFetcher(RemoteNoticeSupport remoteNoticeSupport) {
        this.remoteNoticeSupport = remoteNoticeSupport;
    }

    @Override
    public boolean supports(SubscriptionSource source) {
        return "CANVAS".equalsIgnoreCase(source.getType()) && "CANVAS_API".equalsIgnoreCase(source.getFetchStrategy());
    }

    @Override
    public List<RawNoticeItem> fetch(SubscriptionSource source) {
        Map<String, Object> config = remoteNoticeSupport.readConfig(source);
        String baseUrl = remoteNoticeSupport.firstNonBlank(
                remoteNoticeSupport.stringValue(config.get("baseUrl")),
                source.getSourceUrl());
        List<RawNoticeItem> items = new ArrayList<>();
        if (Boolean.TRUE.equals(config.get("includeGlobalAnnouncements"))) {
            items.addAll(fetchAnnouncements(source, config, baseUrl, List.of("context_codes[]=user_" + remoteNoticeSupport.intValue(config.get("userId"), 1))));
        }
        List<String> courseIds = remoteNoticeSupport.stringList(config.get("announcementCourseIds"));
        if (courseIds.isEmpty()) {
            courseIds = discoverCourseIds(baseUrl, config);
        }
        if (!courseIds.isEmpty()) {
            List<String> query = courseIds.stream().map(id -> "context_codes[]=course_" + id).toList();
            items.addAll(fetchAnnouncements(source, config, baseUrl, query));
        }
        if (!Boolean.FALSE.equals(config.get("includeTodo"))) {
            items.addAll(fetchTodo(source, config, baseUrl));
        }
        return items;
    }

    private List<String> discoverCourseIds(String baseUrl, Map<String, Object> config) {
        String token = remoteNoticeSupport.stringValue(config.get("token"));
        Map<String, Object> requestConfig = new java.util.LinkedHashMap<>(config);
        if (token != null && !token.isBlank()) {
            requestConfig.put("headers", Map.of("Authorization", "Bearer " + token));
        }
        String json = remoteNoticeSupport.fetchText(baseUrl.replaceAll("/$", "") + "/api/v1/courses?enrollment_state=active&per_page=100",
                requestConfig);
        return remoteNoticeSupport.parseJsonArray(json).stream()
                .map(row -> row.get("id"))
                .filter(id -> id != null)
                .map(String::valueOf)
                .collect(Collectors.toList());
    }

    private List<RawNoticeItem> fetchAnnouncements(SubscriptionSource source, Map<String, Object> config,
            String baseUrl, List<String> contextQueries) {
        StringBuilder url = new StringBuilder(baseUrl.replaceAll("/$", "")).append("/api/v1/announcements?");
        for (int i = 0; i < contextQueries.size(); i++) {
            if (i > 0) {
                url.append("&");
            }
            url.append(contextQueries.get(i));
        }
        String token = remoteNoticeSupport.stringValue(config.get("token"));
        Map<String, Object> requestConfig = new java.util.LinkedHashMap<>(config);
        if (token != null && !token.isBlank()) {
            requestConfig.put("headers", Map.of("Authorization", "Bearer " + token));
        }
        String json = remoteNoticeSupport.fetchText(url.toString(), requestConfig);
        List<Map<String, Object>> rows = remoteNoticeSupport.parseJsonArray(json);
        int maxItems = remoteNoticeSupport.intValue(config.get("maxItems"), 30);
        List<RawNoticeItem> items = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            if (items.size() >= maxItems) {
                break;
            }
            String title = remoteNoticeSupport.firstNonBlank(
                    remoteNoticeSupport.stringValue(row.get("title")),
                    remoteNoticeSupport.stringValue(row.get("message")));
            String message = remoteNoticeSupport.cleanText(remoteNoticeSupport.stringValue(row.get("message")));
            items.add(RawNoticeItem.builder()
                    .externalId("canvas-announcement-" + remoteNoticeSupport.stringValue(row.get("id")))
                    .title(title)
                    .content(remoteNoticeSupport.firstNonBlank(message, title))
                    .originalUrl(remoteNoticeSupport.stringValue(row.get("html_url")))
                    .category(remoteNoticeSupport.firstNonBlank(remoteNoticeSupport.stringValue(config.get("category")), "课程"))
                    .publishTime(firstDate(
                            remoteNoticeSupport.parseDate(remoteNoticeSupport.stringValue(row.get("posted_at"))),
                            remoteNoticeSupport.parseDate(remoteNoticeSupport.stringValue(row.get("created_at")))))
                    .rawPayload(row)
                    .tags(List.of("Canvas", "announcement"))
                    .build());
        }
        return items;
    }

    private List<RawNoticeItem> fetchTodo(SubscriptionSource source, Map<String, Object> config, String baseUrl) {
        String token = remoteNoticeSupport.stringValue(config.get("token"));
        Map<String, Object> requestConfig = new java.util.LinkedHashMap<>(config);
        if (token != null && !token.isBlank()) {
            requestConfig.put("headers", Map.of("Authorization", "Bearer " + token));
        }
        String json = remoteNoticeSupport.fetchText(baseUrl.replaceAll("/$", "") + "/api/v1/users/self/todo", requestConfig);
        List<Map<String, Object>> rows = remoteNoticeSupport.parseJsonArray(json);
        int maxItems = remoteNoticeSupport.intValue(config.get("maxItems"), 30);
        List<RawNoticeItem> items = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            if (items.size() >= maxItems) {
                break;
            }
            String assignmentName = nestedValue(row, "assignment", "name");
            String courseName = nestedValue(row, "course", "name");
            String title = remoteNoticeSupport.firstNonBlank(assignmentName, remoteNoticeSupport.stringValue(row.get("type")), "Canvas 待办");
            String content = title + (courseName == null ? "" : " | " + courseName);
            items.add(RawNoticeItem.builder()
                    .externalId("canvas-todo-" + remoteNoticeSupport.stringValue(row.get("assignment_id")))
                    .title(title)
                    .content(content)
                    .originalUrl(nestedValue(row, "html_url"))
                    .category(remoteNoticeSupport.firstNonBlank(remoteNoticeSupport.stringValue(config.get("category")), "课程"))
                    .publishTime(firstDate(
                            remoteNoticeSupport.parseDate(remoteNoticeSupport.stringValue(row.get("due_at"))),
                            remoteNoticeSupport.parseDate(remoteNoticeSupport.stringValue(row.get("created_at")))))
                    .rawPayload(row)
                    .tags(List.of("Canvas", "todo"))
                    .build());
        }
        return items;
    }

    @SuppressWarnings("unchecked")
    private String nestedValue(Map<String, Object> row, String... path) {
        Object current = row;
        for (String key : path) {
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }
            current = map.get(key);
        }
        return current == null ? null : String.valueOf(current);
    }

    private LocalDateTime firstDate(LocalDateTime... values) {
        for (LocalDateTime value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }
}
