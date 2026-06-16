package com.campus.campus_backend.service.info;

import com.campus.campus_backend.domain.SubscriptionSource;
import com.campus.campus_backend.domain.UserCanvasBinding;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.Cookie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TongjiAnnouncementSessionFetcher {
    private static final Logger log = LoggerFactory.getLogger(TongjiAnnouncementSessionFetcher.class);
    private static final String DEFAULT_URL = "https://1.tongji.edu.cn/myAnnouncement";
    private static final Pattern SCRIPT_SRC_PATTERN = Pattern.compile("<script\\b[^>]*src\\s*=\\s*(?:['\"]([^'\"]+)['\"]|([^\\s>]+))[^>]*>",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern MY_ANN_PATTERN = Pattern.compile("([\"'])([^\"']*myAnnouncement[^\"']*)\\1");
    private static final Pattern ANNOUNCEMENT_TAIL_TIME_PATTERN = Pattern.compile("^(.*?)(20\\d{2}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}:\\d{2})\\s*$");
    private static final Pattern ONCLICK_URL_PATTERN = Pattern.compile("(?i)(https?://[^'\"\\s)]+|/[^'\"\\s)]+)");

    private final RemoteNoticeSupport remoteNoticeSupport;

    public TongjiAnnouncementSessionFetcher(RemoteNoticeSupport remoteNoticeSupport) {
        this.remoteNoticeSupport = remoteNoticeSupport;
    }

    public CanvasSessionFetcher.FetchResult fetch(SubscriptionSource source,
            UserCanvasBinding binding,
            CanvasSessionFetcher.LoginResult loginResult,
            boolean debugRaw) {
        List<String> diagnostics = new ArrayList<>(loginResult.getDiagnostics());
        List<RawNoticeItem> items = new ArrayList<>();
        String listUrl = source.getSourceUrl() == null || source.getSourceUrl().isBlank() ? DEFAULT_URL : source.getSourceUrl();
        String html = null;
        try {
            items = fetchViaPlaywrightDom(listUrl, loginResult.getCookies(), diagnostics, debugRaw);
            log.info("TONGJI_DOM_ATTEMPT url={} items={} diagnostics={}", listUrl, items.size(), diagnostics);
            if (!items.isEmpty()) {
                diagnostics.add("tongji-dom-parsed=" + items.size());
                return new CanvasSessionFetcher.FetchResult(items, diagnostics, "FETCH_SUCCESS");
            }

            Map<String, Object> sourceConfig = new LinkedHashMap<>(remoteNoticeSupport.readConfig(source));
            Map<String, Object> headers = new LinkedHashMap<>(remoteNoticeSupport.headers(sourceConfig));
            headers.put("Cookie", cookieHeaderFromSnapshot(loginResult.getCookies()));
            headers.putIfAbsent("Referer", binding.getBaseUrl());
            headers.putIfAbsent("User-Agent", "CampusCircle/1.0");
            sourceConfig.put("headers", headers);
            sourceConfig.putIfAbsent("maxItems", 60);
            sourceConfig.putIfAbsent("category", "閫氱煡");
            sourceConfig.putIfAbsent("baseUrl", "https://1.tongji.edu.cn");
            sourceConfig.putIfAbsent("tags", List.of("Tongji", "鍏憡"));

            html = remoteNoticeSupport.fetchText(listUrl, sourceConfig);
            diagnostics.add("tongji-announcement-page len=" + (html == null ? 0 : html.length()));

            List<RawNoticeItem> parsed = remoteNoticeSupport.parseHtmlAnchors(html, source, sourceConfig);
            items = dedupeAndNormalize(parsed);

            diagnostics.add("tongji-announcement-parsed=" + items.size());
            log.info("TONGJI_ANN_FETCH url={} parsed={} htmlLen={}", listUrl, items.size(), html == null ? 0 : html.length());
            if (items.isEmpty()) {
                log.warn("TONGJI_ANN_EMPTY url={} snippet={}", listUrl, snippet(html));
                inspectSpaScripts(listUrl, sourceConfig, diagnostics);
            }
            String classification;
            if (!items.isEmpty()) {
                classification = "FETCH_SUCCESS";
            } else if (looksLikeShellPage(html)) {
                classification = "TONGJI_SHELL_PAGE";
            } else {
                classification = "TONGJI_EMPTY";
            }
            return new CanvasSessionFetcher.FetchResult(items, diagnostics, classification);
        } catch (Exception e) {
            diagnostics.add("tongji-announcement-error=" + shortMessage(e.getMessage()));
            log.error("TONGJI_ANN_ERROR url={} message={} snippet={}", listUrl, shortMessage(e.getMessage()), snippet(html), e);
            return new CanvasSessionFetcher.FetchResult(List.of(), diagnostics, "TONGJI_FETCH_FAILED");
        }
    }

    private List<RawNoticeItem> fetchViaPlaywrightDom(
            String listUrl,
            Map<String, String> cookieSnapshot,
            List<String> diagnostics,
            boolean debugRaw) {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            try (BrowserContext context = browser.newContext()) {
                List<Cookie> cookies = toPlaywrightCookies(cookieSnapshot);
                if (!cookies.isEmpty()) {
                    context.addCookies(cookies);
                }
                Page page = context.newPage();
                page.setDefaultTimeout(8_000);
                page.setDefaultNavigationTimeout(12_000);
                page.navigate(listUrl, new Page.NavigateOptions().setTimeout(20_000));
                page.waitForLoadState();
                page.waitForLoadState(com.microsoft.playwright.options.LoadState.DOMCONTENTLOADED);
                page.waitForTimeout(2000);
                ensureAnnouncementPanel(page, diagnostics);

                ContainerPick pick = inspectAndSelectAnnouncementContainer(page, diagnostics);
                if (pick == null) {
                    diagnostics.add("tongji-dom-selector-miss");
                } else {
                    List<RawNoticeItem> announcementItems = parseAnnouncementItemsFromSelectedContainer(pick.locator);
                    if (!announcementItems.isEmpty()) {
                        diagnostics.add("tongji-dom-announcement-items=" + announcementItems.size());
                        return dedupeAndNormalize(announcementItems);
                    }

                    String[] rowSelectors = new String[] {
                            ".el-table__body-wrapper tbody tr",
                            ".el-table__body tr",
                            "tbody tr",
                            "tr",
                            "li",
                            ".list-item",
                            ".item"
                    };
                    Locator rows = null;
                    for (String selector : rowSelectors) {
                        Locator candidateRows = pick.locator.locator(selector);
                        int count = candidateRows.count();
                        if (count > 0) {
                            rows = candidateRows;
                            diagnostics.add("tongji-dom-selector=" + selector + " rows=" + count);
                            break;
                        }
                    }
                    if (rows == null) {
                        diagnostics.add("tongji-dom-selector-miss-in-container");
                    } else {
                        List<RawNoticeItem> parsed = parseTableRows(rows, debugRaw);
                        if (!parsed.isEmpty()) {
                            return dedupeAndNormalize(parsed);
                        }
                    }
                }

                diagnostics.add("tongji-dom-fallback-to-eval");
                return dedupeAndNormalize(parseByPageEvaluate(page, diagnostics));
            } finally {
                browser.close();
            }
        } catch (Exception e) {
            diagnostics.add("tongji-dom-error=" + shortMessage(e.getMessage()));
            return List.of();
        }
    }

    private List<RawNoticeItem> parseAnnouncementItemsFromSelectedContainer(Locator container) {
        String[] itemSelectors = new String[] {
                ".noticeList li.announcement-info",
                "ul.infinite-list li.announcement-info",
                "li.announcement-info"
        };
        Locator items = null;
        String selectedSelector = null;
        for (String selector : itemSelectors) {
            Locator candidate = container.locator(selector);
            int count = candidate.count();
            if (count > 0) {
                items = candidate;
                selectedSelector = selector;
                break;
            }
        }
        if (items == null) {
            return List.of();
        }

        int total = Math.min(items.count(), 120);
        List<RawNoticeItem> parsed = new ArrayList<>();
        int skipped = 0;
        for (int i = 0; i < total; i++) {
            Locator item = items.nth(i);
            String rawText = cleanText(safeText(item));
            log.info("TONGJI_DOM_ITEM_RAW idx={} text={}", i + 1, rawText);

            if (rawText.isBlank()) {
                log.warn("TONGJI_DOM_ITEM_PARSE_FAILED idx={} reason=blank-text", i + 1);
                skipped++;
                continue;
            }

            Matcher matcher = ANNOUNCEMENT_TAIL_TIME_PATTERN.matcher(rawText);
            if (!matcher.matches()) {
                log.warn("TONGJI_DOM_ITEM_PARSE_FAILED idx={} reason=time-not-found", i + 1);
                skipped++;
                continue;
            }

            String title = cleanText(matcher.group(1));
            String dateText = cleanText(matcher.group(2));
            if (title.isBlank()) {
                log.warn("TONGJI_DOM_ITEM_PARSE_FAILED idx={} reason=empty-title", i + 1);
                skipped++;
                continue;
            }
            String href = extractHrefFromAnnouncementItem(item);
            RawNoticeItem notice = buildNotice(title, href, dateText, "dom-li");
            parsed.add(notice);
            log.info("TONGJI_DOM_ITEM_PARSED idx={} parsedTitle={} parsedPublishTime={} url={}",
                    i + 1, title, notice.getPublishTime(), notice.getOriginalUrl());
        }
        log.info("TONGJI_DOM_FILTER selector={} rows={} parsed={} skippedNoise={}",
                selectedSelector, total, parsed.size(), skipped);
        return parsed;
    }

    private void ensureAnnouncementPanel(Page page, List<String> diagnostics) {
        try {
            Locator tab = page.locator("text=通知公告");
            if (tab.count() > 0) {
                tab.first().click(new Locator.ClickOptions().setTimeout(5000));
                diagnostics.add("tongji-dom-tab-clicked=通知公告");
                log.info("TONGJI_DOM_TAB clicked=通知公告");
            } else {
                diagnostics.add("tongji-dom-tab-miss=通知公告");
                log.warn("TONGJI_DOM_TAB miss=通知公告");
            }
            page.waitForTimeout(1200);
            Locator annRows = page.locator("table tbody tr");
            annRows.first().waitFor(new Locator.WaitForOptions().setTimeout(8000));
            int rowCount = annRows.count();
            diagnostics.add("tongji-dom-tab-rows=" + rowCount);
            log.info("TONGJI_DOM_TAB rowsAfterClick={}", rowCount);
        } catch (Exception e) {
            diagnostics.add("tongji-dom-tab-error=" + shortMessage(e.getMessage()));
            log.warn("TONGJI_DOM_TAB error={}", shortMessage(e.getMessage()));
        }
    }

    private ContainerPick inspectAndSelectAnnouncementContainer(Page page, List<String> diagnostics) {
        String selector = "table, .el-table, .el-table__body-wrapper, .el-card, .panel, .list, ul, ol, [class*='notice'], [id*='notice'], [class*='announcement'], [id*='announcement']";
        Locator containers = page.locator(selector);
        int total = Math.min(containers.count(), 40);
        ContainerPick best = null;
        for (int i = 0; i < total; i++) {
            Locator container = containers.nth(i);
            String tag = safeLowerAttr(container, "tagName");
            String id = safeAttr(container, "id");
            String clazz = safeAttr(container, "class");
            Locator links = container.locator("a");
            int linkCount = links.count();
            int rowLikeCount = countRowLikeItems(container);
            List<String> firstTexts = collectFirstTexts(container, 3);
            boolean calendarLike = looksLikeCalendarContainer(firstTexts, linkCount);
            int score = scoreContainer(tag, id, clazz, rowLikeCount, linkCount, firstTexts, calendarLike);

            log.info("TONGJI_DOM_CANDIDATE_CONTAINER idx={} tag={} id={} class={} itemCount={} hasA={} score={} first3={}",
                    i, tag, shortValue(id), shortValue(clazz), rowLikeCount, linkCount > 0, score, firstTexts);
            diagnostics.add("tongji-dom-candidate-" + i + " score=" + score + " itemCount=" + rowLikeCount + " links=" + linkCount);

            int linkPreview = Math.min(linkCount, 3);
            for (int j = 0; j < linkPreview; j++) {
                Locator a = links.nth(j);
                String text = cleanText(safeText(a));
                String href = String.valueOf(a.getAttribute("href"));
                log.info("TONGJI_DOM_CANDIDATE_LINK containerIdx={} linkIdx={} text={} href={}",
                        i, j, shortValue(text), shortValue(href));
            }

            if (best == null || score > best.score) {
                best = new ContainerPick(container, i, score, tag, id, clazz);
            }
        }

        if (best != null && best.score >= 2) {
            log.info("TONGJI_DOM_SELECTED_CONTAINER idx={} tag={} id={} class={} score={}",
                    best.index, best.tag, shortValue(best.id), shortValue(best.clazz), best.score);
            diagnostics.add("tongji-dom-selected-container=" + best.index + " score=" + best.score);
            return best;
        }
        return null;
    }

    private int countRowLikeItems(Locator container) {
        String[] itemSelectors = {"tbody tr", "tr", "li", ".list-item", ".item", ".el-table__row"};
        int best = 0;
        for (String itemSelector : itemSelectors) {
            int count = container.locator(itemSelector).count();
            if (count > best) {
                best = count;
            }
        }
        return best;
    }

    private List<String> collectFirstTexts(Locator container, int max) {
        List<String> values = new ArrayList<>();
        String[] itemSelectors = {"tbody tr", "tr", "li", ".list-item", ".item"};
        Locator items = null;
        for (String itemSelector : itemSelectors) {
            Locator c = container.locator(itemSelector);
            if (c.count() > 0) {
                items = c;
                break;
            }
        }
        if (items == null) {
            values.add(cleanText(safeText(container)));
            return values;
        }
        int n = Math.min(items.count(), max);
        for (int i = 0; i < n; i++) {
            values.add(cleanText(safeText(items.nth(i))));
        }
        return values;
    }

    private int scoreContainer(String tag, String id, String clazz, int itemCount, int linkCount, List<String> firstTexts, boolean calendarLike) {
        int score = 0;
        String idLower = id == null ? "" : id.toLowerCase(Locale.ROOT);
        String classLower = clazz == null ? "" : clazz.toLowerCase(Locale.ROOT);
        if (idLower.contains("notice") || idLower.contains("announcement")) {
            score += 5;
        }
        if (classLower.contains("notice") || classLower.contains("announcement")) {
            score += 5;
        }
        if ("table".equals(tag) || classLower.contains("table")) {
            score += 1;
        }
        if (itemCount >= 3 && itemCount <= 80) {
            score += 2;
        }
        if (linkCount > 0) {
            score += 4;
        }
        for (String text : firstTexts) {
            String t = cleanText(text);
            if (containsChinese(t) && t.length() >= 8) {
                score += 2;
            }
            if (t.contains("通知") || t.contains("公告") || t.contains("关于")) {
                score += 3;
            }
        }
        if (calendarLike) {
            score -= 20;
        }
        return score;
    }

    private boolean looksLikeCalendarContainer(List<String> firstTexts, int linkCount) {
        if (firstTexts == null || firstTexts.isEmpty()) {
            return false;
        }
        int calendarSignals = 0;
        for (String text : firstTexts) {
            if (isCalendarLikeRow(text, List.of(), linkCount)) {
                calendarSignals++;
            }
        }
        return calendarSignals >= 2;
    }

    private List<Cookie> toPlaywrightCookies(Map<String, String> snapshot) {
        List<Cookie> cookies = new ArrayList<>();
        if (snapshot == null || snapshot.isEmpty()) {
            return cookies;
        }
        snapshot.forEach((key, value) -> {
            if (key == null || key.isBlank()) {
                return;
            }
            int at = key.indexOf('@');
            if (at <= 0) {
                return;
            }
            String name = key.substring(0, at);
            String domainAndPath = key.substring(at + 1);
            int slash = domainAndPath.indexOf('/');
            String domain = slash >= 0 ? domainAndPath.substring(0, slash) : domainAndPath;
            String path = slash >= 0 ? domainAndPath.substring(slash) : "/";
            if (domain.isBlank() || name.isBlank()) {
                return;
            }
            Cookie c = new Cookie(name, value == null ? "" : value);
            c.setDomain(domain.startsWith(".") ? domain : ("." + domain));
            c.setPath(path.isBlank() ? "/" : path);
            cookies.add(c);
        });
        return cookies;
    }

    private List<RawNoticeItem> parseTableRows(Locator rows, boolean debugRaw) {
        List<RawNoticeItem> parsed = new ArrayList<>();
        int maxRows = Math.min(rows.count(), 100);
        int skippedNoise = 0;
        for (int i = 0; i < maxRows; i++) {
            Locator row = rows.nth(i);
            Locator tds = row.locator("td");
            int tdCount = tds.count();
            if (tdCount < 1) {
                continue;
            }
            List<String> cells = new ArrayList<>();
            for (int c = 0; c < tdCount; c++) {
                cells.add(cleanText(safeText(tds.nth(c))));
            }
            int linkCount = row.locator("a").count();
            String rowText = cleanText(safeText(row));
            if (isCalendarLikeRow(rowText, cells, linkCount)) {
                log.info("TONGJI_DOM_SKIP_CALENDAR_ROW idx={} rowText={} cells={}", i + 1, rowText, cells);
                skippedNoise++;
                continue;
            }
            if (debugRaw) {
                List<String> anchors = new ArrayList<>();
                Locator as = row.locator("a");
                int ac = as.count();
                for (int a = 0; a < ac; a++) {
                    Locator anchor = as.nth(a);
                    anchors.add(cleanText(safeText(anchor)) + " -> " + String.valueOf(anchor.getAttribute("href")));
                }
                log.warn("TONGJI_DEBUG_RAW_ROW idx={} rowText={} cells={} anchors={}",
                        i + 1,
                        rowText,
                        cells,
                        anchors);
            }
            String title = extractBestTitleFromRow(row, tds);
            if (title.isBlank()) {
                skippedNoise++;
                continue;
            }
            String dateText = tdCount >= 2 ? cleanText(safeText(tds.nth(tdCount - 1))) : "";
            String href = selectBestHrefFromRow(row);
            parsed.add(buildNotice(title, href, dateText, "dom-table"));
        }
        log.info("TONGJI_DOM_FILTER rows={} parsed={} skippedNoise={}", maxRows, parsed.size(), skippedNoise);
        return parsed;
    }

    @SuppressWarnings("unchecked")
    private List<RawNoticeItem> parseByPageEvaluate(Page page, List<String> diagnostics) {
        Object result = page.evaluate(
                "() => Array.from(document.querySelectorAll('a')).map(a => ({title:(a.textContent||'').trim(),href:a.getAttribute('href')||'',row:(a.closest('tr')?.innerText||'')}))");
        List<RawNoticeItem> parsed = new ArrayList<>();
        if (!(result instanceof List<?> list)) {
            diagnostics.add("tongji-dom-eval-type-miss");
            return parsed;
        }
        for (Object obj : list) {
            if (!(obj instanceof Map<?, ?> rawMap)) {
                continue;
            }
            Map<String, Object> map = new HashMap<>();
            rawMap.forEach((k, v) -> map.put(String.valueOf(k), v));
            String title = cleanText(String.valueOf(map.getOrDefault("title", "")));
            String href = String.valueOf(map.getOrDefault("href", ""));
            String rowText = cleanText(String.valueOf(map.getOrDefault("row", "")));
            if (title.isBlank()) {
                continue;
            }
            if (!title.contains("閫氱煡") && !title.contains("鍏憡") && !rowText.matches(".*20\\d{2}[-/]\\d{2}[-/]\\d{2}.*")) {
                continue;
            }
            String dateText = extractDateText(rowText);
            parsed.add(buildNotice(title, href, dateText, "dom-eval"));
            if (parsed.size() >= 120) {
                break;
            }
        }
        diagnostics.add("tongji-dom-eval-candidates=" + parsed.size());
        return parsed;
    }

    private RawNoticeItem buildNotice(String title, String href, String dateText, String mode) {
        String fullUrl = resolveUrl("https://1.tongji.edu.cn", href);
        LocalDateTime publishTime = parseDateTime(dateText);
        String externalId = Integer.toHexString(Objects.hash(title, fullUrl, dateText));
        return RawNoticeItem.builder()
                .externalId("tongji-ann-" + externalId)
                .title(title)
                .content(title + (dateText.isBlank() ? "" : (" | " + dateText)))
                .originalUrl(fullUrl)
                .category("閫氱煡")
                .publishTime(publishTime)
                .rawPayload(Map.of("dom", true, "mode", mode, "dateText", dateText))
                .tags(List.of("Tongji", "鍏憡"))
                .build();
    }


    private String selectBestHrefFromRow(Locator row) {
        Locator anchors = row.locator("a");
        int count = anchors.count();
        String fallback = null;
        for (int i = 0; i < count; i++) {
            Locator a = anchors.nth(i);
            String href = a.getAttribute("href");
            String text = cleanText(safeText(a));
            if (href == null || href.isBlank()) {
                continue;
            }
            if (fallback == null) {
                fallback = href;
            }
            if (looksLikeNoticeTitle(text)) {
                return href;
            }
        }
        return fallback;
    }

    private String extractBestTitleFromRow(Locator row, Locator tds) {
        List<String> candidates = new ArrayList<>();
        Locator anchors = row.locator("a");
        int anchorCount = anchors.count();
        for (int i = 0; i < anchorCount; i++) {
            candidates.add(cleanText(safeText(anchors.nth(i))));
        }
        int tdCount = tds.count();
        for (int i = 0; i < tdCount; i++) {
            candidates.add(cleanText(safeText(tds.nth(i))));
        }

        String best = "";
        int bestScore = Integer.MIN_VALUE;
        for (String candidate : candidates) {
            int score = scoreTitleCandidate(candidate);
            if (score > bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return bestScore >= 4 ? best : "";
    }

    private int scoreTitleCandidate(String text) {
        if (text == null) {
            return Integer.MIN_VALUE;
        }
        String value = cleanText(text);
        if (value.isBlank()) {
            return Integer.MIN_VALUE;
        }
        if ("信息标题".equals(value) || "标题".equals(value)) {
            return Integer.MIN_VALUE;
        }
        if (isPureNumberOrPager(value)) {
            return -100;
        }
        int score = 0;
        if (containsChinese(value)) {
            score += 4;
        }
        if (value.length() >= 8) {
            score += 3;
        } else if (value.length() >= 5) {
            score += 1;
        }
        if (looksLikeDateOnly(value)) {
            score -= 3;
        }
        if (value.contains("通知") || value.contains("公告") || value.contains("关于")) {
            score += 2;
        }
        if (value.contains("|")) {
            score -= 2;
        }
        return score;
    }

    private boolean looksLikeNoticeTitle(String text) {
        return scoreTitleCandidate(text) >= 4;
    }

    private boolean isPureNumberOrPager(String text) {
        String value = cleanText(text);
        return value.matches("^\\d+$")
                || value.matches("^\\d+\\s*[|/]\\s*\\d+$")
                || value.matches("^\\d+\\s*[|/]\\s*\\d+\\s*[|/]\\s*\\d+$");
    }

    private boolean looksLikeDateOnly(String text) {
        String value = cleanText(text);
        return value.matches("^20\\d{2}[-/]\\d{1,2}[-/]\\d{1,2}(?:\\s+\\d{1,2}:\\d{2})?$");
    }

    private boolean containsChinese(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        return text.codePoints().anyMatch(cp -> cp >= 0x4E00 && cp <= 0x9FFF);
    }

    private boolean isCalendarLikeRow(String rowText, List<String> cells, int anchorCount) {
        String value = cleanText(rowText);
        if (value.isBlank()) {
            return true;
        }
        String compact = value.replaceAll("\\s+", " ").trim();
        boolean hasCalendarKeyword = compact.contains("劳动节")
                || compact.contains("周一")
                || compact.contains("周二")
                || compact.contains("周三")
                || compact.contains("周四")
                || compact.contains("周五")
                || compact.contains("周六")
                || compact.contains("周日");
        int digitCellCount = 0;
        for (String cell : cells) {
            String c = cleanText(cell);
            if (c.matches("^\\d{1,2}$")) {
                digitCellCount++;
            }
        }
        boolean mostlyDateCells = cells.size() >= 5 && digitCellCount >= Math.max(4, cells.size() - 2);
        boolean numberGridText = compact.matches("^\\d{1,2}(\\s+\\d{1,2}){4,}.*");
        return hasCalendarKeyword || (anchorCount == 0 && (mostlyDateCells || numberGridText));
    }

    private String shortValue(String value) {
        if (value == null) {
            return "";
        }
        String cleaned = cleanText(value);
        return cleaned.length() > 200 ? cleaned.substring(0, 200) : cleaned;
    }

    private String safeAttr(Locator locator, String attr) {
        try {
            String value = locator.getAttribute(attr);
            return value == null ? "" : value;
        } catch (Exception e) {
            return "";
        }
    }

    private String safeLowerAttr(Locator locator, String key) {
        try {
            Object value = locator.evaluate("el => (el.tagName || '').toLowerCase()");
            return value == null ? "" : String.valueOf(value);
        } catch (Exception e) {
            return "";
        }
    }

    private String extractDateText(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        Matcher m = Pattern.compile("(20\\d{2}[-/]\\d{2}[-/]\\d{2}(?:\\s+\\d{2}:\\d{2})?)").matcher(value);
        return m.find() ? m.group(1) : "";
    }

    private List<RawNoticeItem> dedupeAndNormalize(List<RawNoticeItem> items) {
        Set<String> seen = new LinkedHashSet<>();
        List<RawNoticeItem> normalized = new ArrayList<>();
        for (RawNoticeItem item : items) {
            if (item == null) {
                continue;
            }
            String title = safe(item.getTitle());
            String url = safe(item.getOriginalUrl());
            if (title.isBlank()) {
                continue;
            }
            String externalId = item.getExternalId();
            if (externalId == null || externalId.isBlank()) {
                externalId = Integer.toHexString(Objects.hash(title, url, item.getPublishTime()));
            }
            if (!seen.add(externalId)) {
                continue;
            }
            normalized.add(RawNoticeItem.builder()
                    .externalId(externalId.startsWith("tongji-ann-") ? externalId : ("tongji-ann-" + externalId))
                    .title(title)
                    .content(item.getContent())
                    .originalUrl(url.isBlank() ? null : url)
                    .category(item.getCategory())
                    .publishTime(item.getPublishTime())
                    .rawPayload(item.getRawPayload())
                    .tags(item.getTags() == null ? List.of("Tongji", "鍏憡") : item.getTags())
                    .build());
        }
        return normalized;
    }

    private String cookieHeaderFromSnapshot(Map<String, String> snapshot) {
        if (snapshot == null || snapshot.isEmpty()) {
            return "";
        }
        Map<String, String> byName = new LinkedHashMap<>();
        snapshot.forEach((key, value) -> {
            String name = key;
            int idx = key.indexOf('@');
            if (idx > 0) {
                name = key.substring(0, idx);
            }
            if (!name.isBlank() && value != null) {
                byName.putIfAbsent(name, value);
            }
        });
        StringBuilder cookie = new StringBuilder();
        for (Map.Entry<String, String> entry : byName.entrySet()) {
            if (cookie.length() > 0) {
                cookie.append("; ");
            }
            cookie.append(entry.getKey()).append("=").append(entry.getValue());
        }
        return cookie.toString();
    }

    private String snippet(String text) {
        if (text == null || text.isBlank()) {
            return "<empty>";
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        return normalized.length() > 400 ? normalized.substring(0, 400) : normalized;
    }

    private String shortMessage(String message) {
        if (message == null || message.isBlank()) {
            return "unknown";
        }
        return message.length() > 200 ? message.substring(0, 200) : message;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String cleanText(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.replace('\u00A0', ' ').replaceAll("\\s+", " ").trim();
        return fixMojibakeIfNeeded(normalized);
    }

    private String fixMojibakeIfNeeded(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        if (!looksLikeMojibake(text)) {
            return text;
        }
        String recovered = tryTranscode(text, Charset.forName("GBK"), StandardCharsets.UTF_8);
        if (isReadableChinese(recovered) && mojibakeScore(recovered) < mojibakeScore(text)) {
            return recovered;
        }
        return text;
    }

    private boolean looksLikeMojibake(String text) {
        return text.contains("鍏") || text.contains("闁") || text.contains("閫") || text.contains("锟");
    }

    private int mojibakeScore(String text) {
        int score = 0;
        String[] badTokens = {"鍏", "闁", "閫", "锟", "鎴", "鐨", "鏃", "娲"};
        for (String token : badTokens) {
            if (text.contains(token)) {
                score += 2;
            }
        }
        if (containsChinese(text)) {
            score -= 1;
        }
        return score;
    }

    private boolean isReadableChinese(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        if (!containsChinese(text)) {
            return false;
        }
        return !looksLikeMojibake(text);
    }

    private String tryTranscode(String text, Charset from, Charset to) {
        try {
            return new String(text.getBytes(from), to);
        } catch (Exception e) {
            return text;
        }
    }

    private String safeText(Locator locator) {
        try {
            return locator.innerText();
        } catch (Exception e) {
            return "";
        }
    }

    private String firstNonEmpty(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private boolean looksLikeShellPage(String html) {
        String text = html == null ? "" : html.toLowerCase();
        boolean hasAppRoot = text.contains("id=\"app\"") || text.contains("id='app'");
        boolean hasBundleScript = text.contains("main.js") || text.contains("vendor.js") || text.contains("chunk");
        boolean hasVueBootstrap = text.contains("window.__initial_state__") || text.contains("text/javascript");
        boolean hasPortalBranding = text.contains("鍚屾祹澶у") || text.contains("tongji");
        boolean hasPortalShellKeyword = text.contains("portal") && text.contains("announcement");
        return (hasAppRoot && (hasBundleScript || hasVueBootstrap)) || hasPortalShellKeyword || hasPortalBranding;
    }

    private void inspectSpaScripts(String listUrl, Map<String, Object> sourceConfig, List<String> diagnostics) {
        try {
            String html = remoteNoticeSupport.fetchText(listUrl, sourceConfig);
            Matcher m = SCRIPT_SRC_PATTERN.matcher(html);
            List<String> scripts = new ArrayList<>();
            while (m.find() && scripts.size() < 8) {
                String src = m.group(1) != null ? m.group(1) : m.group(2);
                scripts.add(src);
            }
            diagnostics.add("tongji-script-count=" + scripts.size());
            log.info("TONGJI_ANN_SCRIPTS {}", scripts);

            String base = String.valueOf(sourceConfig.getOrDefault("baseUrl", "https://1.tongji.edu.cn"));
            for (String script : scripts) {
                if (script == null || script.isBlank()) {
                    continue;
                }
                String full = script.startsWith("http://") || script.startsWith("https://")
                        ? script
                        : base.replaceAll("/+$", "") + (script.startsWith("/") ? script : "/" + script);
                try {
                    String js = remoteNoticeSupport.fetchText(full, sourceConfig);
                    Matcher endpointMatcher = MY_ANN_PATTERN.matcher(js);
                    Set<String> hits = new LinkedHashSet<>();
                    while (endpointMatcher.find() && hits.size() < 12) {
                        hits.add(endpointMatcher.group(2));
                    }
                    if (!hits.isEmpty()) {
                        diagnostics.add("tongji-js-hit " + full + " => " + hits);
                        log.info("TONGJI_ANN_JS_HIT script={} endpoints={}", full, hits);
                    }
                } catch (Exception ex) {
                    diagnostics.add("tongji-js-read-failed " + full + " err=" + shortMessage(ex.getMessage()));
                }
            }
        } catch (Exception e) {
            diagnostics.add("tongji-spa-inspect-failed " + shortMessage(e.getMessage()));
        }
    }

    private String resolveUrl(String baseUrl, String href) {
        if (href == null || href.isBlank()) {
            return null;
        }
        if (href.startsWith("http://") || href.startsWith("https://")) {
            return href;
        }
        String base = baseUrl == null ? "https://1.tongji.edu.cn" : baseUrl.replaceAll("/+$", "");
        return base + (href.startsWith("/") ? href : "/" + href);
    }

    private String extractHrefFromAnnouncementItem(Locator item) {
        try {
            Locator anchor = item.locator("a[href]");
            if (anchor.count() > 0) {
                String href = anchor.first().getAttribute("href");
                if (href != null && !href.isBlank()) {
                    return href;
                }
            }
            String[] attrs = {"data-href", "data-url", "href", "to"};
            for (String attr : attrs) {
                String value = item.getAttribute(attr);
                if (value != null && !value.isBlank()) {
                    return value;
                }
            }
            String onclick = item.getAttribute("onclick");
            if (onclick != null && !onclick.isBlank()) {
                Matcher m = ONCLICK_URL_PATTERN.matcher(onclick);
                if (m.find()) {
                    return m.group(1);
                }
            }
        } catch (Exception ignore) {
        }
        return null;
    }

    private LocalDateTime parseDateTime(String text) {
        if (text == null || text.isBlank()) {
            return LocalDateTime.now();
        }
        String value = text.trim();
        try {
            if (value.matches("\\d{4}/\\d{2}/\\d{2}")) {
                return LocalDate.parse(value, DateTimeFormatter.ofPattern("yyyy/MM/dd")).atStartOfDay();
            }
            if (value.matches("\\d{4}-\\d{2}-\\d{2}")) {
                return LocalDate.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd")).atStartOfDay();
            }
            if (value.matches("\\d{4}/\\d{2}/\\d{2}\\s+\\d{2}:\\d{2}")) {
                return LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"));
            }
            if (value.matches("\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}")) {
                return LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            }
            if (value.matches("\\d{4}/\\d{2}/\\d{2}\\s+\\d{2}:\\d{2}:\\d{2}")) {
                return LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"));
            }
            if (value.matches("\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}:\\d{2}")) {
                return LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            }
        } catch (Exception ignore) {
        }
        return LocalDateTime.now();
    }

    private static final class ContainerPick {
        private final Locator locator;
        private final int index;
        private final int score;
        private final String tag;
        private final String id;
        private final String clazz;

        private ContainerPick(Locator locator, int index, int score, String tag, String id, String clazz) {
            this.locator = locator;
            this.index = index;
            this.score = score;
            this.tag = tag;
            this.id = id;
            this.clazz = clazz;
        }
    }
}

