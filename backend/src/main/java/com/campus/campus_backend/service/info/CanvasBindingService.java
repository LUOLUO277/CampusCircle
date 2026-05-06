package com.campus.campus_backend.service.info;

import com.campus.campus_backend.common.BizException;
import com.campus.campus_backend.common.ErrorCode;
import com.campus.campus_backend.domain.SubscriptionSource;
import com.campus.campus_backend.domain.User;
import com.campus.campus_backend.domain.UserCanvasBinding;
import com.campus.campus_backend.dto.info.UpsertCanvasBindingRequest;
import com.campus.campus_backend.repository.SubscriptionSourceRepository;
import com.campus.campus_backend.repository.UserCanvasBindingRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class CanvasBindingService {
    public static final String PERSONAL_CANVAS_SOURCE_KEY = "canvas-personal";

    private final UserCanvasBindingRepository userCanvasBindingRepository;
    private final SubscriptionSourceRepository subscriptionSourceRepository;
    private final CanvasSessionFetcher canvasSessionFetcher;
    private final NoticeIngestionService noticeIngestionService;
    private final ObjectMapper objectMapper;

    public CanvasBindingService(UserCanvasBindingRepository userCanvasBindingRepository,
            SubscriptionSourceRepository subscriptionSourceRepository,
            CanvasSessionFetcher canvasSessionFetcher,
            NoticeIngestionService noticeIngestionService,
            ObjectMapper objectMapper) {
        this.userCanvasBindingRepository = userCanvasBindingRepository;
        this.subscriptionSourceRepository = subscriptionSourceRepository;
        this.canvasSessionFetcher = canvasSessionFetcher;
        this.noticeIngestionService = noticeIngestionService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getBinding(User user) {
        return userCanvasBindingRepository.findByUserId(user.getId())
                .map(this::toBindingMap)
                .orElseGet(() -> Map.of(
                        "connected", false,
                        "baseUrl", "https://canvas.tongji.edu.cn",
                        "includeTodo", true,
                        "includeGlobalAnnouncements", true,
                        "courseIds", List.of()));
    }

    @Transactional
    public Map<String, Object> saveBinding(User user, UpsertCanvasBindingRequest request) {
        UserCanvasBinding binding = userCanvasBindingRepository.findByUserId(user.getId())
                .orElseGet(UserCanvasBinding::new);
        binding.setUser(user);
        binding.setBaseUrl(trimTrailingSlash(request.getBaseUrl()));
        binding.setCanvasUsername(request.getUsername().trim());
        String submittedPassword = request.getPassword() == null ? "" : request.getPassword().trim();
        if (submittedPassword.isBlank()) {
            if (binding.getCanvasPassword() == null || binding.getCanvasPassword().isBlank()) {
                throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "Canvas password is required");
            }
        } else {
            binding.setCanvasPassword(submittedPassword);
        }
        binding.setCourseIdsJson(writeCourseIds(request.getCourseIds()));
        binding.setIncludeTodo(!Boolean.FALSE.equals(request.getIncludeTodo()));
        binding.setIncludeGlobalAnnouncements(!Boolean.FALSE.equals(request.getIncludeGlobalAnnouncements()));
        binding.setStatus("ACTIVE");
        binding.setLastSyncStatus(binding.getLastSyncStatus() == null ? "PENDING" : binding.getLastSyncStatus());
        userCanvasBindingRepository.save(binding);
        return toBindingMap(binding);
    }

    @Transactional
    public Map<String, Object> disconnect(User user) {
        UserCanvasBinding binding = userCanvasBindingRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));
        binding.setStatus("DISABLED");
        binding.setSessionCookiesJson(null);
        binding.setLastSyncStatus("DISABLED");
        binding.setLastSyncMessage("User disconnected Canvas");
        userCanvasBindingRepository.save(binding);
        return Map.of("connected", false, "status", "DISABLED");
    }

    @Transactional
    public Map<String, Object> sync(User user) {
        UserCanvasBinding binding = userCanvasBindingRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));
        SubscriptionSource source = ensurePersonalCanvasSource();
        try {
            CanvasSessionFetcher.LoginResult loginResult = canvasSessionFetcher.login(binding);
            binding.setSessionCookiesJson(writeCookies(loginResult.getCookies()));
            binding.setSessionRefreshedAt(LocalDateTime.now());
            CanvasSessionFetcher.FetchResult fetchResult = canvasSessionFetcher.fetch(source, binding, loginResult);
            Map<String, Object> result = noticeIngestionService.ingest(source, user, fetchResult.getItems());
            binding.setLastSyncedAt(LocalDateTime.now());
            binding.setLastSyncStatus(fetchResult.getItems().isEmpty() ? "EMPTY" : "SUCCESS");
            binding.setLastSyncMessage(buildSyncMessage(fetchResult.getClassification(), fetchResult.getDiagnostics()));
            userCanvasBindingRepository.save(binding);
            Map<String, Object> data = new LinkedHashMap<>(result);
            data.put("lastSyncedAt", binding.getLastSyncedAt().toString());
            data.put("classification", fetchResult.getClassification());
            data.put("diagnostics", fetchResult.getDiagnostics());
            return data;
        } catch (CanvasSessionFetcher.CanvasSyncException e) {
            binding.setLastSyncedAt(LocalDateTime.now());
            binding.setLastSyncStatus(e.getClassification());
            binding.setLastSyncMessage(buildSyncMessage(e.getClassification(), e.getDiagnostics()));
            userCanvasBindingRepository.save(binding);
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "Canvas sync failed: " + buildSyncMessage(e.getClassification(), e.getDiagnostics()));
        } catch (Exception e) {
            binding.setLastSyncedAt(LocalDateTime.now());
            binding.setLastSyncStatus("FAILED");
            binding.setLastSyncMessage(shortMessage(e.getMessage()));
            userCanvasBindingRepository.save(binding);
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "Canvas sync failed: " + shortMessage(e.getMessage()));
        }
    }

    @Transactional
    public void syncActiveBindings() {
        for (UserCanvasBinding binding : userCanvasBindingRepository.findByStatusOrderByUpdatedAtDesc("ACTIVE")) {
            try {
                sync(binding.getUser());
            } catch (Exception ignore) {
            }
        }
    }

    private SubscriptionSource ensurePersonalCanvasSource() {
        Optional<SubscriptionSource> existing = subscriptionSourceRepository.findBySourceKey(PERSONAL_CANVAS_SOURCE_KEY);
        if (existing.isPresent()) {
            return existing.get();
        }
        SubscriptionSource source = new SubscriptionSource();
        source.setName("Canvas 个人通知");
        source.setType("CANVAS");
        source.setSourceKey(PERSONAL_CANVAS_SOURCE_KEY);
        source.setSourceUrl("https://canvas.tongji.edu.cn");
        source.setSearchKeywords("canvas personal");
        source.setFetchStrategy("CANVAS_SESSION");
        source.setFetchConfigJson("{}");
        source.setStatus("ACTIVE");
        source.setLastFetchStatus("PENDING");
        return subscriptionSourceRepository.save(source);
    }

    private Map<String, Object> toBindingMap(UserCanvasBinding binding) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("connected", "ACTIVE".equalsIgnoreCase(binding.getStatus()));
        map.put("baseUrl", binding.getBaseUrl());
        map.put("username", binding.getCanvasUsername());
        map.put("hasPassword", binding.getCanvasPassword() != null && !binding.getCanvasPassword().isBlank());
        map.put("hasSession", binding.getSessionCookiesJson() != null && !binding.getSessionCookiesJson().isBlank());
        map.put("courseIds", parseCourseIds(binding.getCourseIdsJson()));
        map.put("includeTodo", binding.getIncludeTodo());
        map.put("includeGlobalAnnouncements", binding.getIncludeGlobalAnnouncements());
        map.put("status", binding.getStatus());
        map.put("lastSyncedAt", binding.getLastSyncedAt() == null ? null : binding.getLastSyncedAt().toString());
        map.put("lastSyncStatus", binding.getLastSyncStatus());
        map.put("lastSyncMessage", binding.getLastSyncMessage());
        map.put("sessionRefreshedAt", binding.getSessionRefreshedAt() == null ? null : binding.getSessionRefreshedAt().toString());
        return map;
    }

    private String writeCourseIds(Long[] courseIds) {
        try {
            List<Long> values = courseIds == null ? List.of()
                    : Arrays.stream(courseIds).filter(id -> id != null && id > 0).distinct().toList();
            return objectMapper.writeValueAsString(values);
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<Long> parseCourseIds(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<Long>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private String writeCookies(Map<String, String> cookies) {
        try {
            return objectMapper.writeValueAsString(cookies);
        } catch (Exception e) {
            return "{}";
        }
    }

    private String trimTrailingSlash(String url) {
        return url == null ? null : url.replaceAll("/+$", "");
    }

    private String shortMessage(String message) {
        if (message == null || message.isBlank()) {
            return "Unknown error";
        }
        return message.length() > 180 ? message.substring(0, 180) : message;
    }

    private String buildSyncMessage(String classification, List<String> diagnostics) {
        String joined = diagnostics == null || diagnostics.isEmpty() ? "" : String.join(" | ", diagnostics);
        String prefix = classification == null ? "Canvas sync diagnostics" : classification;
        String raw = joined.isBlank() ? prefix : prefix + " | " + joined;
        return raw.length() > 500 ? raw.substring(0, 500) : raw;
    }
}
