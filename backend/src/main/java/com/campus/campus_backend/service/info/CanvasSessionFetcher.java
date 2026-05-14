package com.campus.campus_backend.service.info;

import com.campus.campus_backend.domain.SubscriptionSource;
import com.campus.campus_backend.domain.UserCanvasBinding;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class CanvasSessionFetcher {
    private static final Logger log = LoggerFactory.getLogger(CanvasSessionFetcher.class);
    private static final Pattern FORM_ACTION_PATTERN = Pattern.compile("<form[^>]*action=[\"']([^\"']+)[\"'][^>]*>",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern INPUT_TAG_PATTERN = Pattern.compile("<input\\b[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern ATTR_PATTERN = Pattern.compile("([a-zA-Z_:][-a-zA-Z0-9_:.]*)\\s*=\\s*[\"']([^\"']*)[\"']",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern COURSE_LINK_PATTERN = Pattern.compile("/courses/(\\d+)");

    private final RemoteNoticeSupport remoteNoticeSupport;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    /**
     * 用已有 session cookie 调 Canvas JSON API 抓通知
     */
    public FetchResult fetchViaApi(SubscriptionSource source, UserCanvasBinding binding, LoginResult loginResult) {
        String baseUrl = trimTrailingSlash(binding.getBaseUrl());
        List<RawNoticeItem> all = new ArrayList<>();
        List<String> diagnostics = new ArrayList<>(loginResult.diagnostics);

        // 1. 抓 activity_stream（Dashboard 所有通知）
        all.addAll(fetchActivityStream(source, baseUrl, loginResult.cookieJar, diagnostics));

        // 2. 如果 activity_stream 没内容，逐课程抓 announcements API
        if (all.isEmpty()) {
            List<Long> courseIds = discoverCourseIdsViaApi(baseUrl, loginResult.cookieJar, diagnostics);
            diagnostics.add("api-discovered-course-ids=" + courseIds);
            for (Long courseId : courseIds) {
                all.addAll(fetchCourseAnnouncements(source, baseUrl, courseId, loginResult.cookieJar, diagnostics));
            }
        }

        String classification = all.isEmpty() ? "API_FETCH_EMPTY" : "FETCH_SUCCESS";
        diagnostics.add("fetch-classification=" + classification + " total=" + all.size());
        return new FetchResult(all, diagnostics, classification);
    }

    private List<RawNoticeItem> fetchActivityStream(SubscriptionSource source, String baseUrl,
            CookieJar cookies, List<String> diagnostics) {
        try {
            // Canvas API: GET /api/v1/users/self/activity_stream
            String url = baseUrl + "/api/v1/users/self/activity_stream?per_page=50";
            HttpResult result = get(url, Map.of(
                    "Accept", "application/json",
                    "X-Requested-With", "XMLHttpRequest"
            ), cookies);
            diagnostics.add("activity-stream status=" + result.statusCode + " bodyLen=" + bodyLength(result.body));
            if (result.statusCode >= 400 || result.body == null || result.body.isBlank()) {
                return List.of();
            }
            List<Map<String, Object>> items = remoteNoticeSupport.parseJsonArray(result.body);
            List<RawNoticeItem> notices = new ArrayList<>();
            for (Map<String, Object> item : items) {
                String type = remoteNoticeSupport.stringValue(item.get("type"));
                // 只取公告和讨论
                if (!items.isEmpty()) {
                    log.info("CANVAS_STREAM_SAMPLE {}", items.get(0));
                 }
                if (!"Announcement".equals(type) && !"DiscussionTopic".equals(type)) {
                    continue;
                }
                String id = remoteNoticeSupport.stringValue(item.get("id"));
                String title = remoteNoticeSupport.stringValue(item.get("title"));
                String message = remoteNoticeSupport.cleanText(remoteNoticeSupport.stringValue(item.get("message")));
                String htmlUrl = remoteNoticeSupport.stringValue(item.get("html_url"));
                String createdAt = remoteNoticeSupport.stringValue(item.get("created_at"));
                String fullUrl = (htmlUrl != null && htmlUrl.startsWith("/")) ? baseUrl + htmlUrl : htmlUrl;

                notices.add(RawNoticeItem.builder()
                        .externalId(remoteNoticeSupport.firstNonBlank(id, fullUrl, title))
                        .title(title)
                        .content(message != null ? message : title)
                        .originalUrl(fullUrl)
                        .category("课程")
                        .publishTime(remoteNoticeSupport.parseDate(createdAt))
                        .rawPayload(Map.of("canvas_api", true, "type", type))
                        .tags(List.of("Canvas", "课程"))
                        .build());
            }
            diagnostics.add("activity-stream parsed=" + notices.size());
            return notices;
        } catch (Exception e) {
            diagnostics.add("activity-stream error=" + e.getMessage());
            return List.of();
        }
    }

    private List<Long> discoverCourseIdsViaApi(String baseUrl, CookieJar cookies, List<String> diagnostics) {
        try {
            String url = baseUrl + "/api/v1/courses?enrollment_state=active&per_page=50";
            HttpResult result = get(url, Map.of("Accept", "application/json"), cookies);
            List<Map<String, Object>> courses = remoteNoticeSupport.parseJsonArray(result.body);
            return courses.stream()
                    .map(c -> remoteNoticeSupport.stringValue(c.get("id")))
                    .filter(id -> id != null && !id.isBlank())
                    .map(id -> { try { return Long.parseLong(id); } catch (Exception e) { return null; } })
                    .filter(java.util.Objects::nonNull)
                    .toList();
        } catch (Exception e) {
            diagnostics.add("courses-api error=" + e.getMessage());
            return List.of();
        }
    }

    private List<RawNoticeItem> fetchCourseAnnouncements(SubscriptionSource source, String baseUrl,
            Long courseId, CookieJar cookies, List<String> diagnostics) {
        try {
            String url = baseUrl + "/api/v1/announcements?context_codes[]=course_" + courseId + "&per_page=20";
            HttpResult result = get(url, Map.of("Accept", "application/json"), cookies);
            if (result.statusCode >= 400) return List.of();
            List<Map<String, Object>> items = remoteNoticeSupport.parseJsonArray(result.body);
            List<RawNoticeItem> notices = new ArrayList<>();
            for (Map<String, Object> item : items) {
                String id = remoteNoticeSupport.stringValue(item.get("id"));
                String title = remoteNoticeSupport.stringValue(item.get("title"));
                String message = remoteNoticeSupport.cleanText(remoteNoticeSupport.stringValue(item.get("message")));
                String htmlUrl = remoteNoticeSupport.stringValue(item.get("html_url"));
                String postedAt = remoteNoticeSupport.stringValue(item.get("posted_at"));
                String fullUrl = (htmlUrl != null && htmlUrl.startsWith("/")) ? baseUrl + htmlUrl : htmlUrl;
                notices.add(RawNoticeItem.builder()
                        .externalId("course-" + courseId + "-ann-" + id)
                        .title(title)
                        .content(message != null ? message : title)
                        .originalUrl(fullUrl)
                        .category("课程")
                        .publishTime(remoteNoticeSupport.parseDate(postedAt))
                        .rawPayload(Map.of("canvas_api", true, "courseId", courseId))
                        .tags(List.of("Canvas", "课程"))
                        .build());
            }
            diagnostics.add("course-" + courseId + "-announcements parsed=" + notices.size());
            return notices;
        } catch (Exception e) {
            diagnostics.add("course-" + courseId + "-announcements error=" + e.getMessage());
            return List.of();
        }
    }

    public CanvasSessionFetcher(RemoteNoticeSupport remoteNoticeSupport, ObjectMapper objectMapper) {
        this.remoteNoticeSupport = remoteNoticeSupport;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    public LoginResult login(UserCanvasBinding binding) {
        String baseUrl = trimTrailingSlash(binding.getBaseUrl());
        List<String> diagnostics = new ArrayList<>();
        CookieJar cookies = new CookieJar();

        if (binding.getSessionCookiesJson() != null && !binding.getSessionCookiesJson().isBlank()) {
            seedCookiesFromBinding(binding.getSessionCookiesJson(), cookies, diagnostics);
            try {
                HttpResult probe = get(baseUrl, Map.of("Referer", baseUrl), cookies);
                diagnostics.add(describe("session-probe", probe));
                probe = followRedirects(probe, cookies, diagnostics, "session-probe");
                boolean loginLike = looksLikeLoginPage(probe.body) || isIamUrl(probe.url) || (probe.url != null && probe.url.contains("/login"));
                if (!loginLike) {
                    diagnostics.add("login-classification=LOGIN_SUCCESS_REUSED_SESSION");
                    log.info("Canvas login reused session for user {}: {}", binding.getCanvasUsername(), String.join(" | ", diagnostics));
                    return new LoginResult(cookies, probe.url, diagnostics);
                }
            } catch (Exception e) {
                diagnostics.add("session-probe-error=" + shortMessage(e.getMessage()));
            }
        }

        HttpResult current = get(baseUrl, Map.of(), cookies);
        diagnostics.add(describe("entry-home", current));
        current = followRedirects(current, cookies, diagnostics, "entry-home");

        if (!looksLikeCredentialForm(current.body) && !isIamUrl(current.url)) {
            HttpResult samlLogin = get(baseUrl + "/login/saml", Map.of("Referer", baseUrl), cookies);
            diagnostics.add(describe("entry-saml-login", samlLogin));
            samlLogin = followRedirects(samlLogin, cookies, diagnostics, "entry-saml-login");
            if (looksLikeCredentialForm(samlLogin.body) || isIamUrl(samlLogin.url)) {
                current = samlLogin;
            } else {
                HttpResult canvasLogin = get(baseUrl + "/login/canvas", Map.of("Referer", baseUrl), cookies);
                diagnostics.add(describe("entry-canvas-login", canvasLogin));
                current = followRedirects(canvasLogin, cookies, diagnostics, "entry-canvas-login");
            }
        }

        diagnostics.add("credential-inputs=" + describeInputNames(current.body));
        if (!looksLikeCredentialForm(current.body)) {
            diagnostics.add("credential-page-missing-password-field");
            log.warn("Canvas login failed for user {}: {}", binding.getCanvasUsername(), String.join(" | ", diagnostics));
            throw new CanvasSyncException("LOGIN_FAILED", "Could not locate IAM login form.", diagnostics);
        }

        if (requiresHumanVerification(current.body)) {
            diagnostics.add("credential-requires-verification=j_checkcode");
            throw new CanvasSyncException("CAPTCHA_REQUIRED",
                    "Tongji IAM requires verification code (captcha). Automated HTTP login cannot proceed.",
                    diagnostics);
        }

        String actionUrl = resolveUrl(current.url, extractFormAction(current.body, current.url));
        List<FormField> form = extractFormFieldsForSubmit(current.body);
        diagnostics.add("credential-source-fields=" + describeInterestingInputs(current.body));
        List<String> credentialTargets = applyCredentials(form, current.body, binding.getCanvasUsername(), binding.getCanvasPassword());
        ensureField(form, "_eventId", "submit");
        diagnostics.add("credential-action=" + actionUrl + " formFields=" + summarizeFields(form)
                + " credentialTargets=" + credentialTargets
                + " credentialDefaults=" + summarizeCredentialDefaults(form));

        HttpResult submit = postForm(actionUrl, form, Map.of(
                "Referer", current.url,
                "Origin", originOf(actionUrl)
        ), cookies);
        diagnostics.add(describe("credential-submit", submit));
        submit = followRedirects(submit, cookies, diagnostics, "credential-submit");

        HttpResult finalized = resolvePostAuthChain(baseUrl, submit, cookies, diagnostics);
        if (isIamUrl(finalized.url) || looksLikeLoginPage(finalized.body)) {
            diagnostics.add("final-page-title=" + asciiSafe(extractTitle(finalized.body)));
            diagnostics.add("final-page-inputs=" + describeInputNames(finalized.body));
            diagnostics.add("final-page-forms=" + describeForms(finalized.body));
            diagnostics.add("final-page-text=" + asciiSafe(extractVisibleText(finalized.body)));
            diagnostics.add("final-cookie-jar=" + cookies.describeAll());
            diagnostics.add("login-classification=LOGIN_STUCK_ON_IAM");
            log.warn("Canvas login failed for user {}: {}", binding.getCanvasUsername(), String.join(" | ", diagnostics));
            throw new CanvasSyncException("LOGIN_FAILED",
                    "Canvas login did not complete. The session is still on Tongji IAM.",
                    diagnostics);
        }

        diagnostics.add("login-classification=LOGIN_SUCCESS");
        log.info("Canvas login success for user {}: {}", binding.getCanvasUsername(), String.join(" | ", diagnostics));
        return new LoginResult(cookies, finalized.url, diagnostics);
    }

    public FetchResult fetch(SubscriptionSource source, UserCanvasBinding binding, LoginResult loginResult) {
        String baseUrl = trimTrailingSlash(binding.getBaseUrl());
        int maxItems = 50;
        Set<String> seen = new LinkedHashSet<>();
        List<RawNoticeItem> all = new ArrayList<>();
        List<String> diagnostics = new ArrayList<>(loginResult.diagnostics);

        all.addAll(scrapePage(source, baseUrl, baseUrl, loginResult.cookieJar, "课程",
                List.of("todo", "assignment", "quiz", "discussion"), maxItems, seen, diagnostics, "home"));

        if (!Boolean.FALSE.equals(binding.getIncludeTodo())) {
            all.addAll(scrapePage(source, baseUrl, baseUrl + "/calendar", loginResult.cookieJar, "课程",
                    List.of("calendar", "assignment", "event"), maxItems, seen, diagnostics, "calendar"));
        }

        if (!Boolean.FALSE.equals(binding.getIncludeGlobalAnnouncements())) {
            all.addAll(scrapePage(source, baseUrl, baseUrl + "/announcements", loginResult.cookieJar, "课程",
                    List.of("announcement", "通知", "公告"), maxItems, seen, diagnostics, "announcements"));
        }

        List<Long> courseIds = parseCourseIds(binding.getCourseIdsJson());
        diagnostics.add("configured-course-ids=" + courseIds);
        if (courseIds.isEmpty()) {
            HttpResult coursesPage = get(baseUrl + "/courses", Map.of("Referer", baseUrl), loginResult.cookieJar);
            coursesPage = followRedirects(coursesPage, loginResult.cookieJar, diagnostics, "courses-page");
            diagnostics.add(describe("courses-page-final", coursesPage) + " loginLike=" + looksLikeLoginPage(coursesPage.body));
            courseIds = discoverCourseIds(coursesPage.body);
            diagnostics.add("discovered-course-ids=" + courseIds);
            all.addAll(scrapeCourseList(source, baseUrl, coursesPage.body, seen, maxItems, diagnostics));
        }

        for (Long courseId : courseIds) {
            String courseBase = baseUrl + "/courses/" + courseId;
            all.addAll(scrapePage(source, baseUrl, courseBase + "/assignments", loginResult.cookieJar, "课程",
                    List.of("assignment", "作业", "due"), maxItems, seen, diagnostics,
                    "course-" + courseId + "-assignments"));
            all.addAll(scrapePage(source, baseUrl, courseBase + "/discussion_topics", loginResult.cookieJar, "课程",
                    List.of("discussion", "讨论"), maxItems, seen, diagnostics,
                    "course-" + courseId + "-discussions"));
            if (!Boolean.FALSE.equals(binding.getIncludeGlobalAnnouncements())) {
                all.addAll(scrapePage(source, baseUrl, courseBase + "/announcements", loginResult.cookieJar, "课程",
                        List.of("announcement", "通知", "公告"), maxItems, seen, diagnostics,
                        "course-" + courseId + "-announcements"));
            }
        }

        String classification;
        if (all.isEmpty()) {
            boolean iamSeen = diagnostics.stream().anyMatch(item -> item.contains("iam.tongji.edu.cn"));
            boolean noCourses = diagnostics.stream().anyMatch(item -> item.contains("discovered-course-ids=[]"));
            classification = iamSeen ? "LOGIN_SUCCESS_BUT_SESSION_NOT_USABLE"
                    : (noCourses ? "LOGIN_SUCCESS_BUT_NO_COURSE_LINKS" : "LOGIN_SUCCESS_BUT_PAGE_STRUCTURE_UNMATCHED");
            diagnostics.add("fetch-classification=" + classification);
            log.warn("Canvas fetch empty for user {}: {}", binding.getCanvasUsername(), String.join(" | ", diagnostics));
        } else {
            classification = "FETCH_SUCCESS";
            diagnostics.add("fetch-classification=FETCH_SUCCESS total=" + all.size());
            log.info("Canvas fetch success for user {}: {}", binding.getCanvasUsername(), String.join(" | ", diagnostics));
        }

        return new FetchResult(all, diagnostics, classification);
    }

    private HttpResult resolvePostAuthChain(String baseUrl, HttpResult start, CookieJar cookies, List<String> diagnostics) {
        HttpResult current = start;
        for (int i = 0; i < 6; i++) {
            if (looksLikeAutoPostForm(current.body) && !looksLikeCredentialForm(current.body)) {
                String actionUrl = resolveUrl(current.url, extractFormAction(current.body, current.url));
                Map<String, String> hidden = extractHiddenInputs(current.body);
                diagnostics.add("auto-post step=" + i + " action=" + actionUrl + " hiddenKeys=" + hidden.keySet());
                current = postForm(actionUrl, hidden, Map.of(
                        "Referer", current.url,
                        "Origin", originOf(actionUrl)
                ), cookies);
                diagnostics.add(describe("auto-post-result-" + i, current));
                current = followRedirects(current, cookies, diagnostics, "auto-post-result-" + i);
                continue;
            }
            break;
        }
        return current;
    }

    private HttpResult scrapeGet(String baseUrl, String pageUrl, CookieJar cookies, List<String> diagnostics, String label) {
        HttpResult page = get(pageUrl, Map.of("Referer", baseUrl), cookies);
        page = followRedirects(page, cookies, diagnostics, label);
        return page;
    }

    private List<RawNoticeItem> scrapePage(SubscriptionSource source, String baseUrl, String pageUrl,
            CookieJar cookies, String category, List<String> keywords, int maxItems, Set<String> seen,
            List<String> diagnostics, String label) {
        try {
            HttpResult page = scrapeGet(baseUrl, pageUrl, cookies, diagnostics, label);
            boolean loginLike = looksLikeLoginPage(page.body);

            Map<String, Object> config = new LinkedHashMap<>();
            config.put("baseUrl", baseUrl);
            config.put("category", category);
            config.put("maxItems", maxItems);
            config.put("tags", List.of("Canvas", category));
            if (!keywords.isEmpty()) {
                config.put("keyword", String.join(" ", keywords));
            }

            List<RawNoticeItem> parsed = remoteNoticeSupport.parseHtmlAnchors(page.body, source, config);
            List<RawNoticeItem> accepted = new ArrayList<>();
            for (RawNoticeItem item : parsed) {
                String key = item.getExternalId();
                if (key != null && seen.add(key)) {
                    accepted.add(item);
                }
            }
            diagnostics.add(describe(label + "-final", page) + " parsed=" + parsed.size() + " accepted=" + accepted.size() + " loginLike=" + loginLike);
            return accepted;
        } catch (Exception e) {
            diagnostics.add(label + " error=" + shortMessage(e.getMessage()));
            return List.of();
        }
    }

    private List<RawNoticeItem> scrapeCourseList(SubscriptionSource source, String baseUrl, String html,
            Set<String> seen, int maxItems, List<String> diagnostics) {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("baseUrl", baseUrl);
        config.put("category", "课程");
        config.put("maxItems", maxItems);
        config.put("tags", List.of("Canvas", "course"));
        config.put("keyword", "course 课程");
        List<RawNoticeItem> parsed = remoteNoticeSupport.parseHtmlAnchors(html, source, config);
        List<RawNoticeItem> accepted = new ArrayList<>();
        for (RawNoticeItem item : parsed) {
            if (item.getOriginalUrl() != null && item.getOriginalUrl().contains("/courses/") && seen.add(item.getExternalId())) {
                accepted.add(item);
            }
        }
        diagnostics.add("courses-list parsed=" + parsed.size() + " accepted=" + accepted.size());
        return accepted;
    }

    private HttpResult followRedirects(HttpResult start, CookieJar cookies, List<String> diagnostics, String label) {
        HttpResult current = start;
        for (int i = 0; i < 8; i++) {
            if (!isRedirect(current.statusCode) || current.location == null || current.location.isBlank()) {
                return current;
            }
            String nextUrl = resolveUrl(current.url, current.location);
            diagnostics.add(label + "-redirect-" + i + " status=" + current.statusCode + " -> " + nextUrl);
            current = get(nextUrl, Map.of("Referer", current.url), cookies);
            diagnostics.add(describe(label + "-redirect-target-" + i, current));
        }
        return current;
    }

    private HttpResult get(String url, Map<String, String> headers, CookieJar cookies) {
        try {
            URI requestUri = URI.create(url);
            String cookieHeader = cookies.headerValue(requestUri);
            HttpRequest.Builder builder = HttpRequest.newBuilder(requestUri)
                    .GET()
                    .timeout(Duration.ofSeconds(20))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                    .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                    .header("Upgrade-Insecure-Requests", "1");
            headers.forEach(builder::header);
            if (!cookieHeader.isBlank()) {
                builder.header("Cookie", cookieHeader);
            }
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            HttpResult result = toResult(response);
            result.requestCookieSummary = cookies.describeFor(requestUri);
            result.responseCookieSummary = describeResponseCookies(result.cookies);
            cookies.mergeFrom(requestUri, result.cookies);
            result.jarCookieSummary = cookies.describeFor(requestUri);
            return result;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to fetch page: " + url, e);
        }
    }

    private HttpResult postForm(String url, Map<String, String> form, Map<String, String> headers, CookieJar cookies) {
        try {
            URI requestUri = URI.create(url);
            String cookieHeader = cookies.headerValue(requestUri);
            HttpRequest.Builder builder = HttpRequest.newBuilder(requestUri)
                    .POST(HttpRequest.BodyPublishers.ofString(formUrlEncode(form)))
                    .timeout(Duration.ofSeconds(20))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
            headers.forEach(builder::header);
            if (!cookieHeader.isBlank()) {
                builder.header("Cookie", cookieHeader);
            }
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            HttpResult result = toResult(response);
            result.requestCookieSummary = cookies.describeFor(requestUri);
            result.responseCookieSummary = describeResponseCookies(result.cookies);
            cookies.mergeFrom(requestUri, result.cookies);
            result.jarCookieSummary = cookies.describeFor(requestUri);
            return result;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to submit login form", e);
        }
    }

    private HttpResult postForm(String url, List<FormField> form, Map<String, String> headers, CookieJar cookies) {
        try {
            URI requestUri = URI.create(url);
            String cookieHeader = cookies.headerValue(requestUri);
            HttpRequest.Builder builder = HttpRequest.newBuilder(requestUri)
                    .POST(HttpRequest.BodyPublishers.ofString(formUrlEncode(form)))
                    .timeout(Duration.ofSeconds(20))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
            headers.forEach(builder::header);
            if (!cookieHeader.isBlank()) {
                builder.header("Cookie", cookieHeader);
            }
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            HttpResult result = toResult(response);
            result.requestCookieSummary = cookies.describeFor(requestUri);
            result.responseCookieSummary = describeResponseCookies(result.cookies);
            cookies.mergeFrom(requestUri, result.cookies);
            result.jarCookieSummary = cookies.describeFor(requestUri);
            return result;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to submit login form", e);
        }
    }

    private HttpResult toResult(HttpResponse<String> response) {
        return new HttpResult(
                response.uri().toString(),
                response.body(),
                extractCookies(response.headers()),
                response.statusCode(),
                response.headers().firstValue("location").orElse(null),
                response.headers().firstValue("content-type").orElse(null));
    }

    private Map<String, String> extractHiddenInputs(String html) {
        Map<String, String> values = new LinkedHashMap<>();
        for (Map<String, String> input : parseInputs(html)) {
            if ("hidden".equalsIgnoreCase(input.getOrDefault("type", "")) && input.containsKey("name")) {
                values.put(input.get("name"), input.getOrDefault("value", ""));
            }
        }
        return values;
    }

    private List<FormField> extractFormFieldsForSubmit(String html) {
        List<FormField> values = new ArrayList<>();
        for (Map<String, String> input : parseInputs(html)) {
            String name = input.get("name");
            if (name == null || name.isBlank()) {
                continue;
            }
            String type = input.getOrDefault("type", "text").toLowerCase();
            if (type.equals("hidden") || type.equals("text") || type.equals("password")
                    || type.equals("email") || type.equals("tel") || type.equals("search")
                    || type.equals("number") || type.equals("url")
                    || type.equals("submit") || type.equals("button")) {
                values.add(new FormField(name, input.getOrDefault("value", "")));
            }
        }
        return values;
    }

    private String extractFormAction(String html, String fallback) {
        Matcher matcher = FORM_ACTION_PATTERN.matcher(Objects.toString(html, ""));
        return matcher.find() ? htmlDecode(matcher.group(1)) : fallback;
    }

    private String detectUsernameField(String html) {
        String fallback = null;
        for (Map<String, String> input : parseInputs(html)) {
            String type = input.getOrDefault("type", "text").toLowerCase();
            String name = input.get("name");
            if (name == null || name.isBlank()) {
                continue;
            }
            String lower = name.toLowerCase();
            if (fallback == null && ("text".equals(type) || "email".equals(type) || "tel".equals(type))) {
                fallback = name;
            }
            if (lower.contains("user") || lower.contains("login") || lower.contains("account")
                    || lower.contains("mobile") || lower.contains("phone")) {
                return name;
            }
        }
        return fallback != null ? fallback : "username";
    }

    private String detectPasswordField(String html) {
        for (Map<String, String> input : parseInputs(html)) {
            String type = input.getOrDefault("type", "").toLowerCase();
            String name = input.get("name");
            if ("password".equals(type) && name != null && !name.isBlank()) {
                return name;
            }
        }
        return "password";
    }

    private List<String> applyCredentials(List<FormField> form, String html, String username, String password) {
        List<String> touched = new ArrayList<>();
        Set<String> usernameFields = new LinkedHashSet<>();
        Set<String> passwordFields = new LinkedHashSet<>();
        String detectedUsernameField = detectUsernameField(html);
        if (detectedUsernameField != null && !detectedUsernameField.isBlank()) {
            usernameFields.add(detectedUsernameField);
        }
        String detectedPasswordField = detectPasswordField(html);
        if (detectedPasswordField != null && !detectedPasswordField.isBlank()) {
            passwordFields.add(detectedPasswordField);
        }
        for (Map<String, String> input : parseInputs(html)) {
            String name = input.get("name");
            if (name == null || name.isBlank()) {
                continue;
            }
            String lower = name.toLowerCase();
            String type = input.getOrDefault("type", "text").toLowerCase();
            if (("text".equals(type) || "email".equals(type) || "tel".equals(type))
                    && (lower.contains("user") || lower.contains("login") || lower.contains("account")
                    || lower.equals("fs12_username") || lower.equals("fs1_username"))) {
                usernameFields.add(name);
            }
            if ("password".equals(type)
                    || lower.equals("j_password")
                    || lower.equals("fs1_password")
                    || lower.equals("fs1_passwordnew")
                    || lower.equals("fs1_passwordqr")) {
                passwordFields.add(name);
            }
        }
        for (FormField field : form) {
            if (usernameFields.contains(field.name)) {
                field.value = username;
            }
            if (passwordFields.contains(field.name)) {
                field.value = password;
            }
        }
        for (String field : usernameFields) {
            touched.add(field + "=<username>");
        }
        for (String field : passwordFields) {
            touched.add(field + "=<password>");
        }

        boolean removedOp = false;
        boolean removedPasswordInputId = false;
        boolean removedCheckcode = false;
        boolean blankedAuthenticatedUsername = false;
        for (int i = form.size() - 1; i >= 0; i--) {
            FormField field = form.get(i);
            if ("op".equals(field.name) && looksSuspiciousOpValue(field.value)) {
                form.remove(i);
                removedOp = true;
                continue;
            }
            if ("passwordInputID".equals(field.name) && (field.value == null || field.value.isBlank())) {
                form.remove(i);
                removedPasswordInputId = true;
                continue;
            }
            if ("j_checkcode".equals(field.name) && looksLikePlaceholder(field.value)) {
                form.remove(i);
                removedCheckcode = true;
            }
        }
        if (removedOp) {
            touched.add("op=<removed>");
        }
        if (removedPasswordInputId) {
            touched.add("passwordInputID=<removed>");
        }
        if (removedCheckcode) {
            touched.add("j_checkcode=<removed>");
        }
        return touched;
    }

    private boolean looksLikePlaceholder(String value) {
        if (value == null) {
            return true;
        }
        String trimmed = value.trim();
        if (trimmed.isBlank()) {
            return true;
        }
        String lower = trimmed.toLowerCase();
        return lower.contains("请输入") || lower.contains("please") || lower.contains("captcha") || lower.contains("checkcode")
                || lower.contains("验证码");
    }

    private boolean looksSuspiciousOpValue(String opValue) {
        if (opValue == null || opValue.isBlank()) {
            return false;
        }
        String lower = opValue.toLowerCase();
        return lower.contains("bind_openid") || lower.contains("openid_uid");
    }

    private String describeInterestingInputs(String html) {
        List<String> values = new ArrayList<>();
        for (Map<String, String> input : parseInputs(html)) {
            String name = input.get("name");
            if (name == null || name.isBlank()) {
                continue;
            }
            String lower = name.toLowerCase();
            if (lower.equals("op") || lower.contains("user") || lower.contains("password")
                    || lower.contains("auth") || lower.contains("checkcode")) {
                String type = input.getOrDefault("type", "text");
                String value = input.getOrDefault("value", "");
                if (lower.contains("password")) {
                    value = value.isBlank() ? "<blank>" : "<masked>";
                } else if (value.isBlank()) {
                    value = "<blank>";
                }
                values.add(name + ":" + type + "=" + value);
                if (values.size() >= 20) {
                    break;
                }
            }
        }
        return values.isEmpty() ? "<none>" : values.toString();
    }

    private boolean looksLikeCredentialForm(String html) {
        return parseInputs(html).stream()
                .anyMatch(input -> "password".equalsIgnoreCase(input.getOrDefault("type", "")));
    }

    private boolean requiresHumanVerification(String html) {
        for (Map<String, String> input : parseInputs(html)) {
            String name = input.get("name");
            if (name == null) {
                continue;
            }
            if ("j_checkcode".equalsIgnoreCase(name)) {
                String type = input.getOrDefault("type", "text").toLowerCase();
                if (!"hidden".equals(type)) {
                    return true;
                }
            }
        }
        String lower = Objects.toString(html, "").toLowerCase();
        return lower.contains("验证码") || lower.contains("checkcode");
    }

    private void seedCookiesFromBinding(String sessionCookiesJson, CookieJar cookies, List<String> diagnostics) {
        Map<String, String> snapshot = parseCookieSnapshot(sessionCookiesJson);
        if (snapshot.isEmpty()) {
            diagnostics.add("session-seed=<empty>");
            return;
        }
        int added = 0;
        for (Map.Entry<String, String> entry : snapshot.entrySet()) {
            CookieParts parts = CookieParts.parse(entry.getKey());
            if (parts == null) {
                continue;
            }
            cookies.addManual(parts.name, entry.getValue(), parts.domain, parts.path, true);
            added++;
        }
        diagnostics.add("session-seed-added=" + added);
    }

    private Map<String, String> parseCookieSnapshot(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, String>>() {});
        } catch (Exception ignore) {
            log.warn("Failed to parse sessionCookiesJson (len={}): {}", json.length(), shortMessage(ignore.getMessage()));
            return Map.of();
        }
    }

    private static final class CookieParts {
        private final String name;
        private final String domain;
        private final String path;

        private CookieParts(String name, String domain, String path) {
            this.name = name;
            this.domain = domain;
            this.path = path;
        }

        private static CookieParts parse(String key) {
            if (key == null || key.isBlank()) {
                return null;
            }
            int at = key.indexOf('@');
            if (at <= 0 || at >= key.length() - 1) {
                return null;
            }
            String name = key.substring(0, at);
            String rest = key.substring(at + 1);
            int slash = rest.indexOf('/');
            if (slash <= 0) {
                return null;
            }
            String domain = rest.substring(0, slash).toLowerCase();
            String path = rest.substring(slash);
            if (!path.startsWith("/")) {
                path = "/" + path;
            }
            return new CookieParts(name, domain, path);
        }
    }

    private boolean looksLikeAutoPostForm(String html) {
        String text = Objects.toString(html, "");
        return text.contains("SAMLResponse") || (text.contains("<form") && !text.contains("type=\"password\""));
    }

    private boolean looksLikeLoginPage(String html) {
        String lower = Objects.toString(html, "").toLowerCase();
        return lower.contains("name=\"password\"")
                || lower.contains("统一身份认证")
                || lower.contains("cas login")
                || lower.contains("authnengine")
                || lower.contains("请输入密码")
                || lower.contains("canvas lms")
                || lower.contains("/login/openid_connect")
                || lower.contains("/login/canvas")
                || lower.contains("/login/saml");
    }

    private boolean isIamUrl(String url) {
        return url != null && url.contains("iam.tongji.edu.cn");
    }

    private boolean isRedirect(int statusCode) {
        return statusCode == 301 || statusCode == 302 || statusCode == 303 || statusCode == 307 || statusCode == 308;
    }

    private List<HttpCookie> extractCookies(HttpHeaders headers) {
        List<HttpCookie> cookies = new ArrayList<>();
        for (String header : headers.allValues("Set-Cookie")) {
            try {
                cookies.addAll(HttpCookie.parse(header));
            } catch (IllegalArgumentException ignore) {
            }
        }
        return cookies;
    }

    private List<Long> discoverCourseIds(String html) {
        Set<Long> ids = new LinkedHashSet<>();
        Matcher matcher = COURSE_LINK_PATTERN.matcher(Objects.toString(html, ""));
        while (matcher.find()) {
            try {
                ids.add(Long.parseLong(matcher.group(1)));
            } catch (NumberFormatException ignore) {
            }
        }
        return new ArrayList<>(ids);
    }

    private List<Long> parseCourseIds(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(json, new com.fasterxml.jackson.core.type.TypeReference<List<Long>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private String resolveUrl(String baseOrCurrentUrl, String action) {
        if (action == null || action.isBlank()) {
            return baseOrCurrentUrl;
        }
        try {
            return URI.create(baseOrCurrentUrl).resolve(htmlDecode(action)).toString();
        } catch (Exception e) {
            return action;
        }
    }

    private String originOf(String url) {
        URI uri = URI.create(url);
        return uri.getScheme() + "://" + uri.getHost() + (uri.getPort() > 0 ? ":" + uri.getPort() : "");
    }

    private String trimTrailingSlash(String url) {
        return url == null ? null : url.replaceAll("/+$", "");
    }

    private String formUrlEncode(Map<String, String> form) {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : form.entrySet()) {
            if (!first) {
                builder.append("&");
            }
            builder.append(encode(entry.getKey())).append("=").append(encode(entry.getValue()));
            first = false;
        }
        return builder.toString();
    }

    private String formUrlEncode(List<FormField> form) {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (FormField field : form) {
            if (!first) {
                builder.append("&");
            }
            builder.append(encode(field.name)).append("=").append(encode(field.value));
            first = false;
        }
        return builder.toString();
    }

    private void ensureField(List<FormField> form, String name, String value) {
        boolean found = false;
        for (FormField field : form) {
            if (name.equals(field.name)) {
                field.value = value;
                found = true;
            }
        }
        if (!found) {
            form.add(new FormField(name, value));
        }
    }

    private String summarizeFields(List<FormField> fields) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (FormField field : fields) {
            counts.merge(field.name, 1, Integer::sum);
        }
        return "total=" + fields.size() + " names=" + counts;
    }

    private String encode(String value) {
        try {
            return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            return value;
        }
    }

    private String describe(String label, HttpResult result) {
        return label + " status=" + result.statusCode + " url=" + result.url + " location=" + safe(result.location)
                + " contentType=" + safe(result.contentType) + " bodyLength=" + bodyLength(result.body)
                + " snippet=" + snippet(result.body)
                + " requestCookies=" + safe(result.requestCookieSummary)
                + " responseCookies=" + safe(result.responseCookieSummary)
                + " jarCookies=" + safe(result.jarCookieSummary);
    }

    private String safe(String value) {
        return value == null ? "<none>" : value;
    }

    private int bodyLength(String body) {
        return body == null ? 0 : body.length();
    }

    private String snippet(String body) {
        if (body == null || body.isBlank()) {
            return "<empty>";
        }
        String text = body.replaceAll("\\s+", " ").trim();
        return text.length() > 120 ? text.substring(0, 120) : text;
    }

    private String extractVisibleText(String body) {
        if (body == null || body.isBlank()) {
            return "<empty>";
        }
        String text = body
                .replaceAll("(?is)<script\\b[^>]*>.*?</script>", " ")
                .replaceAll("(?is)<style\\b[^>]*>.*?</style>", " ")
                .replaceAll("(?is)<[^>]+>", " ")
                .replace("&nbsp;", " ");
        text = htmlDecode(text).replaceAll("\\s+", " ").trim();
        return text.length() > 400 ? text.substring(0, 400) : text;
    }

    private String shortMessage(String message) {
        if (message == null || message.isBlank()) {
            return "unknown";
        }
        return message.length() > 180 ? message.substring(0, 180) : message;
    }

    private List<Map<String, String>> parseInputs(String html) {
        List<Map<String, String>> inputs = new ArrayList<>();
        Matcher tagMatcher = INPUT_TAG_PATTERN.matcher(Objects.toString(html, ""));
        while (tagMatcher.find()) {
            String tag = tagMatcher.group();
            Map<String, String> attrs = new LinkedHashMap<>();
            Matcher attrMatcher = ATTR_PATTERN.matcher(tag);
            while (attrMatcher.find()) {
                attrs.put(attrMatcher.group(1).toLowerCase(), htmlDecode(attrMatcher.group(2)));
            }
            if (!attrs.isEmpty()) {
                inputs.add(attrs);
            }
        }
        return inputs;
    }

    private String htmlDecode(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return value
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&apos;", "'");
    }

    private String describeInputNames(String html) {
        List<String> names = new ArrayList<>();
        for (Map<String, String> input : parseInputs(html)) {
            String name = input.getOrDefault("name", "<unnamed>");
            String type = input.getOrDefault("type", "text");
            names.add(name + ":" + type);
            if (names.size() >= 12) {
                break;
            }
        }
        return names.isEmpty() ? "<none>" : names.toString();
    }

    private String summarizeCredentialDefaults(List<FormField> form) {
        List<String> values = new ArrayList<>();
        for (FormField field : form) {
            String key = field.name;
            String value = field.value;
            if (key == null || key.isBlank()) {
                continue;
            }
            String lower = key.toLowerCase();
            if (lower.contains("user") || lower.contains("password") || lower.equals("op") || lower.contains("auth")) {
                String normalized = value;
                if (lower.contains("password")) {
                    normalized = "<password>";
                } else if (lower.contains("user")) {
                    normalized = "<username>";
                } else if (normalized == null || normalized.isBlank()) {
                    normalized = "<blank>";
                }
                values.add(key + "=" + normalized);
                if (values.size() >= 12) {
                    break;
                }
            }
        }
        return values.isEmpty() ? "<none>" : values.toString();
    }

    private String summarizeCredentialDefaults(Map<String, String> form) {
        List<String> values = new ArrayList<>();
        for (Map.Entry<String, String> entry : form.entrySet()) {
            String lower = entry.getKey().toLowerCase();
            if (lower.contains("user") || lower.contains("password") || lower.equals("op") || lower.contains("auth")) {
                String value = entry.getValue();
                if (lower.contains("password")) {
                    value = "<password>";
                } else if (lower.contains("user")) {
                    value = "<username>";
                } else if (value == null || value.isBlank()) {
                    value = "<blank>";
                }
                values.add(entry.getKey() + "=" + value);
                if (values.size() >= 12) {
                    break;
                }
            }
        }
        return values.isEmpty() ? "<none>" : values.toString();
    }

    private String extractTitle(String html) {
        Matcher matcher = Pattern.compile("<title[^>]*>(.*?)</title>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)
                .matcher(Objects.toString(html, ""));
        if (!matcher.find()) {
            return "<none>";
        }
        return matcher.group(1).replaceAll("\\s+", " ").trim();
    }

    private String describeForms(String html) {
        Matcher matcher = Pattern.compile("<form\\b[^>]*action=[\"']([^\"']*)[\"'][^>]*>", Pattern.CASE_INSENSITIVE)
                .matcher(Objects.toString(html, ""));
        List<String> actions = new ArrayList<>();
        while (matcher.find() && actions.size() < 4) {
            actions.add(htmlDecode(matcher.group(1)));
        }
        return actions.isEmpty() ? "<none>" : actions.toString();
    }

    private String describeResponseCookies(List<HttpCookie> cookies) {
        if (cookies == null || cookies.isEmpty()) {
            return "<none>";
        }
        List<String> summary = new ArrayList<>();
        for (HttpCookie cookie : cookies) {
            summary.add(cookie.getName() + "@" + safeCookieDomain(cookie.getDomain()) + safeCookiePath(cookie.getPath()));
            if (summary.size() >= 8) {
                break;
            }
        }
        return summary.toString();
    }

    private String safeCookieDomain(String domain) {
        return (domain == null || domain.isBlank()) ? "<host>" : domain;
    }

    private String safeCookiePath(String path) {
        return (path == null || path.isBlank()) ? "/" : path;
    }

    private String asciiSafe(String value) {
        if (value == null) {
            return "<none>";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch >= 32 && ch <= 126) {
                builder.append(ch);
            } else {
                builder.append(String.format("\\u%04x", (int) ch));
            }
        }
        return builder.toString();
    }

    public static final class LoginResult {
        private final CookieJar cookieJar;
        private final String finalUrl;
        private final List<String> diagnostics;

        public LoginResult(CookieJar cookieJar, String finalUrl, List<String> diagnostics) {
            this.cookieJar = cookieJar;
            this.finalUrl = finalUrl;
            this.diagnostics = diagnostics;
        }

        public Map<String, String> getCookies() {
            return cookieJar.asMap();
        }

        public String getFinalUrl() {
            return finalUrl;
        }

        public List<String> getDiagnostics() {
            return diagnostics;
        }
    }

    public static final class FetchResult {
        private final List<RawNoticeItem> items;
        private final List<String> diagnostics;
        private final String classification;

        public FetchResult(List<RawNoticeItem> items, List<String> diagnostics, String classification) {
            this.items = items;
            this.diagnostics = diagnostics;
            this.classification = classification;
        }

        public List<RawNoticeItem> getItems() {
            return items;
        }

        public List<String> getDiagnostics() {
            return diagnostics;
        }

        public String getClassification() {
            return classification;
        }
    }

    public static final class CanvasSyncException extends RuntimeException {
        private final String classification;
        private final List<String> diagnostics;

        public CanvasSyncException(String classification, String message, List<String> diagnostics) {
            super(message);
            this.classification = classification;
            this.diagnostics = diagnostics;
        }

        public String getClassification() {
            return classification;
        }

        public List<String> getDiagnostics() {
            return diagnostics;
        }
    }

    private static final class FormField {
        private final String name;
        private String value;

        private FormField(String name, String value) {
            this.name = name;
            this.value = value;
        }
    }

    private static final class HttpResult {
        private final String url;
        private final String body;
        private final List<HttpCookie> cookies;
        private final int statusCode;
        private final String location;
        private final String contentType;
        private String requestCookieSummary;
        private String responseCookieSummary;
        private String jarCookieSummary;

        private HttpResult(String url, String body, List<HttpCookie> cookies, int statusCode, String location, String contentType) {
            this.url = url;
            this.body = body;
            this.cookies = cookies;
            this.statusCode = statusCode;
            this.location = location;
            this.contentType = contentType;
        }
    }

    private static final class CookieJar {
        private final List<StoredCookie> cookies = new ArrayList<>();

        private void addManual(String name, String value, String domain, String path, boolean secure) {
            if (name == null || name.isBlank()) {
                return;
            }
            String normalizedDomain = Optional.ofNullable(domain).orElse("").toLowerCase();
            if (normalizedDomain.startsWith(".")) {
                normalizedDomain = normalizedDomain.substring(1);
            }
            String normalizedPath = (path == null || path.isBlank()) ? "/" : path;
            if (!normalizedPath.startsWith("/")) {
                normalizedPath = "/" + normalizedPath;
            }
            StoredCookie stored = new StoredCookie(name, value == null ? "" : value, normalizedDomain, normalizedPath, secure, -1,
                    System.currentTimeMillis());
            cookies.removeIf(existing -> existing.sameIdentity(stored));
            cookies.add(stored);
        }

        private void mergeFrom(URI requestUri, List<HttpCookie> responseCookies) {
            if (responseCookies == null || responseCookies.isEmpty()) {
                return;
            }
            for (HttpCookie cookie : responseCookies) {
                if (cookie == null || cookie.getName() == null || cookie.getName().isBlank()) {
                    continue;
                }
                StoredCookie stored = StoredCookie.from(requestUri, cookie);
                cookies.removeIf(existing -> existing.sameIdentity(stored));
                if (!stored.expired()) {
                    cookies.add(stored);
                }
            }
        }

        private String headerValue(URI requestUri) {
            List<String> values = new ArrayList<>();
            for (StoredCookie cookie : cookies) {
                if (cookie.matches(requestUri) && !cookie.expired()) {
                    values.add(cookie.name + "=" + cookie.value);
                }
            }
            return String.join("; ", values);
        }

        private String describeFor(URI requestUri) {
            List<String> values = new ArrayList<>();
            for (StoredCookie cookie : cookies) {
                if (cookie.matches(requestUri) && !cookie.expired()) {
                    values.add(cookie.name + "@" + cookie.domain + cookie.path);
                    if (values.size() >= 8) {
                        break;
                    }
                }
            }
            return values.isEmpty() ? "<none>" : values.toString();
        }

        private Map<String, String> asMap() {
            Map<String, String> snapshot = new LinkedHashMap<>();
            for (StoredCookie cookie : cookies) {
                if (!cookie.expired()) {
                    snapshot.put(cookie.name + "@" + cookie.domain + cookie.path, cookie.value);
                }
            }
            return snapshot;
        }

        private String describeAll() {
            List<String> values = new ArrayList<>();
            for (StoredCookie cookie : cookies) {
                if (!cookie.expired()) {
                    values.add(cookie.name + "@" + cookie.domain + cookie.path);
                    if (values.size() >= 16) {
                        break;
                    }
                }
            }
            return values.isEmpty() ? "<none>" : values.toString();
        }
    }

    private static final class StoredCookie {
        private final String name;
        private final String value;
        private final String domain;
        private final String path;
        private final boolean secure;
        private final long maxAge;
        private final long storedAtMillis;

        private StoredCookie(String name, String value, String domain, String path, boolean secure, long maxAge, long storedAtMillis) {
            this.name = name;
            this.value = value;
            this.domain = domain;
            this.path = path;
            this.secure = secure;
            this.maxAge = maxAge;
            this.storedAtMillis = storedAtMillis;
        }

        private static StoredCookie from(URI requestUri, HttpCookie cookie) {
            String requestPath = Optional.ofNullable(requestUri.getPath()).filter(path -> !path.isBlank()).orElse("/");
            String domain = Optional.ofNullable(cookie.getDomain()).filter(value -> !value.isBlank())
                    .map(String::toLowerCase)
                    .orElseGet(() -> Optional.ofNullable(requestUri.getHost()).orElse("").toLowerCase());
            String path = Optional.ofNullable(cookie.getPath()).filter(value -> !value.isBlank())
                    .orElseGet(() -> defaultPath(requestPath));
            return new StoredCookie(cookie.getName(), cookie.getValue(), domain, path, cookie.getSecure(), cookie.getMaxAge(),
                    System.currentTimeMillis());
        }

        private static String defaultPath(String requestPath) {
            int slash = requestPath.lastIndexOf('/');
            if (slash <= 0) {
                return "/";
            }
            return requestPath.substring(0, slash + 1);
        }

        private boolean sameIdentity(StoredCookie other) {
            return name.equals(other.name) && domain.equals(other.domain) && path.equals(other.path);
        }

        private boolean matches(URI requestUri) {
            String host = Optional.ofNullable(requestUri.getHost()).orElse("").toLowerCase();
            String requestPath = Optional.ofNullable(requestUri.getPath()).filter(path -> !path.isBlank()).orElse("/");
            String normalizedDomain = domain.startsWith(".") ? domain.substring(1) : domain;
            boolean hostMatch = host.equals(normalizedDomain) || host.endsWith("." + normalizedDomain);
            boolean pathMatch = requestPath.startsWith(path);
            boolean secureMatch = !secure || "https".equalsIgnoreCase(requestUri.getScheme());
            return hostMatch && pathMatch && secureMatch;
        }

        private boolean expired() {
            if (maxAge == 0) {
                return true;
            }
            if (maxAge < 0) {
                return false;
            }
            return System.currentTimeMillis() - storedAtMillis > maxAge * 1000;
        }
    }
}
