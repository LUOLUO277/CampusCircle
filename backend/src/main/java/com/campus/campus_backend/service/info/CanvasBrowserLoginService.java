package com.campus.campus_backend.service.info;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.WaitForSelectorState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class CanvasBrowserLoginService {
    private static final Logger log = LoggerFactory.getLogger(CanvasBrowserLoginService.class);

    public Map<String, String> loginAndExportCookieSnapshot(String baseUrl, String username, String password, Duration timeout) {
        String normalizedBaseUrl = trimTrailingSlash(baseUrl);
        Instant deadline = Instant.now().plus(timeout == null ? Duration.ofMinutes(3) : timeout);

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                    .setHeadless(false));

            try (BrowserContext context = browser.newContext()) {
                Page page = context.newPage();
                page.navigate(normalizedBaseUrl);

                waitForAnySelector(page, Duration.ofSeconds(20),
                        "input[name='j_username']",
                        "input[name='username']",
                        "input[type='password']");

                fillIfPresent(page, "input[name='j_username']", username);
                fillIfPresent(page, "input[name='username']", username);
                fillIfPresent(page, "input[name='j_password']", password);
                fillIfPresent(page, "input[name='password']", password);

                log.warn("Canvas browser login: please complete IAM verification (captcha/MFA) in the opened browser window within {} seconds.",
                        Duration.between(Instant.now(), deadline).toSeconds());

                while (Instant.now().isBefore(deadline)) {
                    String currentUrl = page.url();
                    if (looksLoggedInCanvasUrl(currentUrl)) {
                        Map<String, String> snapshot = CookieSnapshot.fromContext(context);
                        log.warn("Canvas browser login: detected logged-in Canvas page url={}; captured cookies={}", currentUrl, snapshot.keySet());
                        return snapshot;
                    }
                    page.waitForTimeout(500);
                }

                throw new IllegalStateException("Browser login timeout: did not reach logged-in Canvas page within " + timeout);
            } finally {
                browser.close();
            }
        }
    }

    private void waitForAnySelector(Page page, Duration timeout, String... selectors) {
        Instant deadline = Instant.now().plus(timeout);
        while (Instant.now().isBefore(deadline)) {
            for (String selector : selectors) {
                try {
                    if (page.querySelector(selector) != null) {
                        return;
                    }
                } catch (Exception ignore) {
                }
            }
            page.waitForTimeout(250);
        }
    }

    private void fillIfPresent(Page page, String selector, String value) {
        if (value == null) {
            return;
        }
        try {
            if (page.querySelector(selector) == null) {
                return;
            }
            page.waitForSelector(selector, new Page.WaitForSelectorOptions()
                    .setState(WaitForSelectorState.VISIBLE)
                    .setTimeout(5_000));
            page.fill(selector, value);
        } catch (Exception ignore) {
        }
    }

    private boolean looksLoggedInCanvasUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        String lower = url.toLowerCase();
        if (!lower.contains("canvas.tongji.edu.cn")) {
            return false;
        }
        return !lower.contains("/login") && !lower.contains("iam.tongji.edu.cn");
    }

    private String trimTrailingSlash(String url) {
        return url == null ? null : url.replaceAll("/+$", "");
    }

    private static final class CookieSnapshot {
        private static Map<String, String> fromContext(BrowserContext context) {
            Map<String, String> snapshot = new LinkedHashMap<>();
            for (com.microsoft.playwright.options.Cookie cookie : context.cookies()) {
                if (cookie == null || cookie.name == null || cookie.name.isBlank()) {
                    continue;
                }
                String domain = cookie.domain == null ? "" : cookie.domain.toLowerCase();
                String path = cookie.path == null || cookie.path.isBlank() ? "/" : cookie.path;
                String key = cookie.name + "@" + normalizeDomain(domain) + ensureLeadingSlash(path);
                snapshot.put(key, cookie.value == null ? "" : cookie.value);
            }
            return snapshot;
        }

        private static String normalizeDomain(String domain) {
            String value = domain == null ? "" : domain.trim().toLowerCase();
            if (value.startsWith(".")) {
                value = value.substring(1);
            }
            return value;
        }

        private static String ensureLeadingSlash(String path) {
            if (path == null || path.isBlank()) {
                return "/";
            }
            return path.startsWith("/") ? path : "/" + path;
        }
    }
}

