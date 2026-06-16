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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class CanvasBindingService {
    private static final Logger log = LoggerFactory.getLogger(CanvasBindingService.class);
    public static final String PERSONAL_CANVAS_SOURCE_KEY = "canvas-personal";
    public static final String PERSONAL_TONGJI_ANNOUNCEMENT_SOURCE_KEY = "tongji-announcement-personal";
    private static final int MAX_COOKIE_SNAPSHOT_LEN = 20000;
    private static final int MAX_COURSE_IDS_JSON_LEN = 4000;
    private static final Map<Long, ReentrantLock> USER_SYNC_LOCKS = new ConcurrentHashMap<>();

    private final UserCanvasBindingRepository userCanvasBindingRepository;
    private final SubscriptionSourceRepository subscriptionSourceRepository;
    private final CanvasSessionFetcher canvasSessionFetcher;
    private final TongjiSsoSessionService tongjiSsoSessionService;
    private final TongjiAnnouncementSessionFetcher tongjiAnnouncementSessionFetcher;
    private final CanvasBrowserLoginService canvasBrowserLoginService;
    private final NoticeIngestionService noticeIngestionService;
    private final ObjectMapper objectMapper;

    public CanvasBindingService(UserCanvasBindingRepository userCanvasBindingRepository,
            SubscriptionSourceRepository subscriptionSourceRepository,
            CanvasSessionFetcher canvasSessionFetcher,
            TongjiSsoSessionService tongjiSsoSessionService,
            TongjiAnnouncementSessionFetcher tongjiAnnouncementSessionFetcher,
            CanvasBrowserLoginService canvasBrowserLoginService,
            NoticeIngestionService noticeIngestionService,
            ObjectMapper objectMapper) {
        this.userCanvasBindingRepository = userCanvasBindingRepository;
        this.subscriptionSourceRepository = subscriptionSourceRepository;
        this.canvasSessionFetcher = canvasSessionFetcher;
        this.tongjiSsoSessionService = tongjiSsoSessionService;
        this.tongjiAnnouncementSessionFetcher = tongjiAnnouncementSessionFetcher;
        this.canvasBrowserLoginService = canvasBrowserLoginService;
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

    public Map<String, Object> sync(User user) {
        return sync(user, "all", false, false);
    }

    public Map<String, Object> sync(User user, boolean forceRelogin) {
        return sync(user, "all", forceRelogin, false);
    }

    public Map<String, Object> sync(User user, String source, boolean forceRelogin) {
        return sync(user, source, forceRelogin, false);
    }

    public Map<String, Object> sync(User user, String source, boolean forceRelogin, boolean debugRaw) {
        log.error("SYNC_SERVICE_ENTRY userId={} source={} forceRelogin={} debugRaw={}",
                user.getId(), source, forceRelogin, debugRaw);
        ReentrantLock lock = USER_SYNC_LOCKS.computeIfAbsent(user.getId(), ignored -> new ReentrantLock());
        if (!lock.tryLock()) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "Sync already in progress, please wait");
        }
        try {
        UserCanvasBinding binding = userCanvasBindingRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));
        SubscriptionSource canvasSource = ensurePersonalCanvasSource();
        SubscriptionSource tongjiAnnouncementSource = ensurePersonalTongjiAnnouncementSource();
        String normalizedSource = source == null ? "all" : source.trim().toLowerCase();
        if (!List.of("all", "canvas", "tongji").contains(normalizedSource)) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "Unsupported source: " + source);
        }
        boolean runCanvas = "all".equals(normalizedSource) || "canvas".equals(normalizedSource);
        boolean runTongji = "all".equals(normalizedSource) || "tongji".equals(normalizedSource);

        Map<String, Object> canvasResult = runCanvas
                ? syncCanvasSource(user, binding, canvasSource, forceRelogin)
                : skippedSourceResult("SKIPPED");
        Map<String, Object> tongjiResult = runTongji
                ? syncTongjiSource(user, binding, tongjiAnnouncementSource, forceRelogin, debugRaw)
                : skippedSourceResult("SKIPPED");

        int totalFetched = intValue(canvasResult.get("fetched")) + intValue(tongjiResult.get("fetched"));
        int successCount = intValue(canvasResult.get("successCount")) + intValue(tongjiResult.get("successCount"));
        int failureCount = intValue(canvasResult.get("failureCount")) + intValue(tongjiResult.get("failureCount"));
        String overallStatus = resolveOverallStatus(canvasResult, tongjiResult, runCanvas, runTongji);

        binding.setLastSyncedAt(LocalDateTime.now());
        binding.setLastSyncStatus(overallStatus);
        binding.setLastSyncMessage(buildSyncMessage(overallStatus, mergeDiagnostics(
                toStringList(canvasResult.get("diagnostics")),
                toStringList(tongjiResult.get("diagnostics")))));
        userCanvasBindingRepository.save(binding);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("canvas", canvasResult);
        data.put("tongjiAnnouncement", tongjiResult);
        data.put("overallStatus", overallStatus);
        data.put("totalFetched", totalFetched);
        data.put("successCount", successCount);
        data.put("failureCount", failureCount);
        data.put("lastSyncedAt", binding.getLastSyncedAt().toString());
        return data;
        } finally {
            lock.unlock();
        }
    }

    private Map<String, Object> syncCanvasSource(User user,
            UserCanvasBinding binding,
            SubscriptionSource canvasSource,
            boolean forceRelogin) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            CanvasSessionFetcher.LoginResult canvasLoginResult = tongjiSsoSessionService.login(binding, forceRelogin);
            binding.setSessionCookiesJson(writeCookies(canvasLoginResult.getCookies()));
            binding.setSessionRefreshedAt(LocalDateTime.now());
            CanvasSessionFetcher.FetchResult canvasFetchResult = canvasSessionFetcher.fetchViaApi(canvasSource, binding, canvasLoginResult);
            Map<String, Object> ingest = noticeIngestionService.ingest(canvasSource, user, canvasFetchResult.getItems());
            result.putAll(ingest);
            result.put("status", "SUCCESS");
            result.put("fetched", canvasFetchResult.getItems().size());
            result.put("classification", canvasFetchResult.getClassification());
            result.put("diagnostics", canvasFetchResult.getDiagnostics());
            return result;
        } catch (CanvasSessionFetcher.CanvasSyncException e) {
            String status = requiresBrowserLogin(e.getClassification(), e.getDiagnostics()) ? "NEED_BROWSER_LOGIN" : "FAILED";
            result.put("status", status);
            result.put("fetched", 0);
            result.put("successCount", 0);
            result.put("failureCount", 0);
            result.put("classification", e.getClassification());
            result.put("diagnostics", e.getDiagnostics() == null ? List.of() : e.getDiagnostics());
            return result;
        } catch (Exception e) {
            result.put("status", "FAILED");
            result.put("fetched", 0);
            result.put("successCount", 0);
            result.put("failureCount", 0);
            result.put("classification", "FAILED");
            result.put("diagnostics", List.of(shortMessage(e.getMessage())));
            return result;
        }
    }

    private Map<String, Object> syncTongjiSource(User user,
            UserCanvasBinding binding,
            SubscriptionSource tongjiAnnouncementSource,
            boolean forceRelogin,
            boolean debugRaw) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            CanvasSessionFetcher.LoginResult tongjiLoginResult = tongjiSsoSessionService.login(asTongjiPortalBinding(binding), forceRelogin);
            binding.setSessionCookiesJson(writeCookies(tongjiLoginResult.getCookies()));
            binding.setSessionRefreshedAt(LocalDateTime.now());
            CanvasSessionFetcher.FetchResult tongjiFetchResult = tongjiAnnouncementSessionFetcher.fetch(
                    tongjiAnnouncementSource, binding, tongjiLoginResult, debugRaw);
            if (debugRaw) {
                List<RawNoticeItem> previewItems = tongjiFetchResult.getItems() == null
                        ? List.of()
                        : tongjiFetchResult.getItems();
                log.warn("TONGJI_DEBUG_RAW_RESULT userId={} fetched={} classification={}",
                        user.getId(), previewItems.size(), tongjiFetchResult.getClassification());
                for (int i = 0; i < previewItems.size(); i++) {
                    RawNoticeItem item = previewItems.get(i);
                    log.warn("TONGJI_DEBUG_RAW_ITEM idx={} title={} publishTime={} url={} contentSnippet={}",
                            i + 1,
                            truncateForClient(item.getTitle(), 120),
                            item.getPublishTime(),
                            item.getOriginalUrl(),
                            truncateForClient(item.getContent(), 160));
                }
                result.put("successCount", 0);
                result.put("failureCount", 0);
                result.put("debugItems", previewItems.stream().limit(120).map(item -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("externalId", item.getExternalId());
                    row.put("title", item.getTitle());
                    row.put("content", item.getContent());
                    row.put("originalUrl", item.getOriginalUrl());
                    row.put("publishTime", item.getPublishTime() == null ? null : item.getPublishTime().toString());
                    row.put("rawPayload", item.getRawPayload());
                    return row;
                }).toList());
            } else {
                Map<String, Object> ingest = noticeIngestionService.ingest(
                        tongjiAnnouncementSource, user, tongjiFetchResult.getItems());
                result.putAll(ingest);
            }
            String classification = tongjiFetchResult.getClassification();
            String status = "TONGJI_SHELL_PAGE".equals(classification)
                    ? "TONGJI_SHELL_PAGE"
                    : (debugRaw ? "DEBUG_RAW" : "SUCCESS");
            result.put("status", status);
            result.put("fetched", tongjiFetchResult.getItems().size());
            result.put("classification", classification);
            result.put("diagnostics", tongjiFetchResult.getDiagnostics());
            return result;
        } catch (CanvasSessionFetcher.CanvasSyncException e) {
            String status = requiresBrowserLogin(e.getClassification(), e.getDiagnostics()) ? "NEED_BROWSER_LOGIN" : "FAILED";
            result.put("status", status);
            result.put("fetched", 0);
            result.put("successCount", 0);
            result.put("failureCount", 0);
            result.put("classification", e.getClassification());
            result.put("diagnostics", e.getDiagnostics() == null ? List.of() : e.getDiagnostics());
            return result;
        } catch (Exception e) {
            result.put("status", "FAILED");
            result.put("fetched", 0);
            result.put("successCount", 0);
            result.put("failureCount", 0);
            result.put("classification", "FAILED");
            result.put("diagnostics", List.of(shortMessage(e.getMessage())));
            return result;
        }
    }

    private Map<String, Object> skippedSourceResult(String status) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", status);
        result.put("fetched", 0);
        result.put("successCount", 0);
        result.put("failureCount", 0);
        result.put("diagnostics", List.of());
        result.put("classification", status);
        return result;
    }

    private String resolveOverallStatus(Map<String, Object> canvasResult,
            Map<String, Object> tongjiResult,
            boolean runCanvas,
            boolean runTongji) {
        List<String> activeStatuses = new ArrayList<>();
        if (runCanvas) {
            activeStatuses.add(String.valueOf(canvasResult.getOrDefault("status", "FAILED")));
        }
        if (runTongji) {
            activeStatuses.add(String.valueOf(tongjiResult.getOrDefault("status", "FAILED")));
        }
        boolean allSuccess = activeStatuses.stream().allMatch("SUCCESS"::equals);
        if (allSuccess) {
            return "SUCCESS";
        }
        boolean anySuccess = activeStatuses.stream().anyMatch("SUCCESS"::equals);
        return anySuccess ? "PARTIAL_SUCCESS" : "FAILED";
    }

    private boolean requiresBrowserLogin(String classification, List<String> diagnostics) {
        if (classification == null) {
            return false;
        }
        if ("CAPTCHA_REQUIRED".equals(classification) || "LOGIN_FAILED".equals(classification)) {
            return true;
        }
        if (diagnostics == null) {
            return false;
        }
        return diagnostics.stream().anyMatch(item -> {
            if (item == null) {
                return false;
            }
            String lower = item.toLowerCase();
            return lower.contains("401")
                    || lower.contains("403")
                    || lower.contains("iam.tongji.edu.cn")
                    || lower.contains("login");
        });
    }

    @SuppressWarnings("unchecked")
    private List<String> toStringList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().filter(item -> item != null && !String.valueOf(item).isBlank())
                    .map(String::valueOf).toList();
        }
        return List.of();
    }

    @Transactional
    public Map<String, Object> browserLogin(User user) {
        UserCanvasBinding binding = userCanvasBindingRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));
        if (binding.getBaseUrl() == null || binding.getBaseUrl().isBlank()) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "Canvas baseUrl is required");
        }
        if (binding.getCanvasUsername() == null || binding.getCanvasUsername().isBlank()) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "Canvas username is required");
        }
        if (binding.getCanvasPassword() == null || binding.getCanvasPassword().isBlank()) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "Canvas password is required");
        }

        try {
            Map<String, String> cookies = canvasBrowserLoginService.loginAndExportCookieSnapshot(
                    binding.getBaseUrl(),
                    binding.getCanvasUsername(),
                    binding.getCanvasPassword(),
                    java.time.Duration.ofMinutes(3));
            binding.setSessionCookiesJson(writeCookies(cookies));
            binding.setSessionRefreshedAt(LocalDateTime.now());
            binding.setLastSyncStatus("SESSION_REFRESHED");
            binding.setLastSyncMessage(truncateForClient("Browser login succeeded. Session cookies refreshed=" + cookies.keySet(), 500));
            userCanvasBindingRepository.save(binding);
            return toBindingMap(binding);
        } catch (Exception e) {
            binding.setLastSyncStatus("BROWSER_LOGIN_FAILED");
            binding.setLastSyncMessage(shortMessage(e.getMessage()));
            userCanvasBindingRepository.save(binding);
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "Canvas browser login failed: " + shortMessage(e.getMessage()));
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
            SubscriptionSource source = existing.get();
            noticeIngestionService.normalizeSourceForStorage(source);
            return subscriptionSourceRepository.save(source);
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
        noticeIngestionService.normalizeSourceForStorage(source);
        return subscriptionSourceRepository.save(source);
    }

    private SubscriptionSource ensurePersonalTongjiAnnouncementSource() {
        Optional<SubscriptionSource> existing = subscriptionSourceRepository.findBySourceKey(PERSONAL_TONGJI_ANNOUNCEMENT_SOURCE_KEY);
        if (existing.isPresent()) {
            SubscriptionSource source = existing.get();
            noticeIngestionService.normalizeSourceForStorage(source);
            return subscriptionSourceRepository.save(source);
        }
        SubscriptionSource source = new SubscriptionSource();
        source.setName("同济门户个人公告");
        source.setType("TONGJI");
        source.setSourceKey(PERSONAL_TONGJI_ANNOUNCEMENT_SOURCE_KEY);
        source.setSourceUrl("https://1.tongji.edu.cn/myAnnouncement");
        source.setSearchKeywords("tongji portal announcement");
        source.setFetchStrategy("TONGJI_ANNOUNCEMENT_SESSION");
        source.setFetchConfigJson("{\"maxItems\":60,\"category\":\"通知\",\"baseUrl\":\"https://1.tongji.edu.cn\",\"tags\":[\"Tongji\",\"公告\"]}");
        source.setStatus("ACTIVE");
        source.setLastFetchStatus("PENDING");
        noticeIngestionService.normalizeSourceForStorage(source);
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
            return truncateForStorage(objectMapper.writeValueAsString(values), MAX_COURSE_IDS_JSON_LEN);
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
            return truncateForStorage(objectMapper.writeValueAsString(cookies), MAX_COOKIE_SNAPSHOT_LEN);
        } catch (Exception e) {
            return "{}";
        }
    }

    private String truncateForStorage(String value, int maxLen) {
        if (value == null || value.length() <= maxLen) {
            return value;
        }
        return value.substring(0, maxLen);
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
        return truncateForClient(raw, 500);
    }

    private String truncateForClient(String value, int maxLen) {
        if (value == null) {
            return null;
        }
        if (value.length() <= maxLen) {
            return value;
        }
        int head = Math.min(220, maxLen / 2);
        int tail = Math.min(260, maxLen - head - 5);
        if (tail <= 0) {
            return value.substring(0, maxLen);
        }
        return value.substring(0, head) + " ... " + value.substring(value.length() - tail);
    }

    private List<String> mergeDiagnostics(List<String> canvasDiagnostics, List<String> tongjiDiagnostics) {
        List<String> merged = new java.util.ArrayList<>();
        if (canvasDiagnostics != null) {
            canvasDiagnostics.forEach(item -> merged.add("canvas: " + item));
        }
        if (tongjiDiagnostics != null) {
            tongjiDiagnostics.forEach(item -> merged.add("tongji: " + item));
        }
        return merged;
    }

    private int intValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return value == null ? 0 : Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            return 0;
        }
    }

    private UserCanvasBinding asTongjiPortalBinding(UserCanvasBinding original) {
        UserCanvasBinding binding = new UserCanvasBinding();
        binding.setUser(original.getUser());
        binding.setCanvasUsername(original.getCanvasUsername());
        binding.setCanvasPassword(original.getCanvasPassword());
        binding.setSessionCookiesJson(original.getSessionCookiesJson());
        binding.setBaseUrl("https://1.tongji.edu.cn");
        binding.setIncludeTodo(original.getIncludeTodo());
        binding.setIncludeGlobalAnnouncements(original.getIncludeGlobalAnnouncements());
        binding.setCourseIdsJson(original.getCourseIdsJson());
        binding.setStatus(original.getStatus());
        return binding;
    }
}
