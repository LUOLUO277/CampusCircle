package com.campus.campus_backend.service.info;

import com.campus.campus_backend.domain.SubscriptionSource;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class RemoteNoticeSupport {
    private static final Pattern TAG_PATTERN = Pattern.compile("<[^>]+>");
    private static final Pattern BROKEN_TAG_PATTERN = Pattern.compile("<[^\\n>]{1,200}");
    private static final Pattern SCRIPT_PATTERN = Pattern.compile("<script[\\s\\S]*?</script>", Pattern.CASE_INSENSITIVE);
    private static final Pattern STYLE_PATTERN = Pattern.compile("<style[\\s\\S]*?</style>", Pattern.CASE_INSENSITIVE);
    private static final Pattern SPACE_PATTERN = Pattern.compile("\\s+");
    private static final Pattern DATE_PATTERN = Pattern.compile("(20\\d{2}[-/.年]\\d{1,2}[-/.月]\\d{1,2}(?:日)?(?:\\s+\\d{1,2}:\\d{2})?)");
    private static final Pattern ANCHOR_PATTERN = Pattern.compile(
            "<a\\b[^>]*href\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>([\\s\\S]*?)</a>",
            Pattern.CASE_INSENSITIVE);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public RemoteNoticeSupport(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public Map<String, Object> readConfig(SubscriptionSource source) {
        if (source.getFetchConfigJson() == null || source.getFetchConfigJson().isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(source.getFetchConfigJson(), new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    public String fetchText(String url, Map<String, Object> config) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                    .GET()
                    .timeout(Duration.ofSeconds(intValue(config.get("timeoutSeconds"), 15)));
            headers(config).forEach(builder::header);
            HttpResponse<byte[]> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() >= 400) {
                throw new IllegalStateException("Remote request failed: " + response.statusCode() + " " + url);
            }
            Charset charset = detectCharset(response);
            return new String(response.body(), charset);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to fetch remote content: " + url, e);
        }
    }

    private Charset detectCharset(HttpResponse<?> response) {
        try {
            String contentType = response.headers().firstValue("Content-Type").orElse("");
            int index = contentType.toLowerCase(Locale.ROOT).indexOf("charset=");
            if (index >= 0) {
                String value = contentType.substring(index + "charset=".length()).trim();
                int semicolon = value.indexOf(';');
                if (semicolon > 0) {
                    value = value.substring(0, semicolon).trim();
                }
                value = value.replace("\"", "");
                if (!value.isBlank()) {
                    return Charset.forName(value);
                }
            }
        } catch (Exception ignore) {
        }
        return StandardCharsets.UTF_8;
    }

    public List<RawNoticeItem> parseRss(String xml, SubscriptionSource source, Map<String, Object> config) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
            NodeList items = document.getElementsByTagName("item");
            if (items.getLength() == 0) {
                items = document.getElementsByTagName("entry");
            }
            int maxItems = intValue(config.get("maxItems"), 20);
            List<RawNoticeItem> results = new ArrayList<>();
            for (int i = 0; i < items.getLength() && results.size() < maxItems; i++) {
                Element element = (Element) items.item(i);
                String title = firstText(element, "title");
                String link = firstLink(element);
                String guid = firstNonBlank(firstText(element, "guid"), link, title, source.getSourceKey() + "-" + i);
                String content = firstNonBlank(firstText(element, "description"), firstText(element, "content:encoded"),
                        firstText(element, "summary"), title);
                results.add(RawNoticeItem.builder()
                        .externalId(guid)
                        .title(cleanText(title))
                        .content(cleanText(content))
                        .originalUrl(link)
                        .category(stringValue(config.get("category")))
                        .publishTime(parseDate(firstNonBlank(firstText(element, "pubDate"), firstText(element, "updated"),
                                firstText(element, "published"))))
                        .rawPayload(Map.of("rss", true, "sourceUrl", source.getSourceUrl()))
                        .tags(tagsFromConfig(config))
                        .build());
            }
            return results;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse RSS feed for source " + source.getSourceKey(), e);
        }
    }

    public List<RawNoticeItem> parseHtmlAnchors(String html, SubscriptionSource source, Map<String, Object> config) {
        Matcher matcher = ANCHOR_PATTERN.matcher(html);
        String keyword = firstNonBlank(stringValue(config.get("keyword")), source.getSearchKeywords());
        Set<String> seen = new LinkedHashSet<>();
        List<RawNoticeItem> items = new ArrayList<>();
        int maxItems = intValue(config.get("maxItems"), 20);
        while (matcher.find() && items.size() < maxItems) {
            String href = absolutizeUrl(firstNonBlank(stringValue(config.get("baseUrl")), source.getSourceUrl()), matcher.group(1));
            String title = cleanText(matcher.group(2));
            if (title == null || title.isBlank()) {
                continue;
            }
            if (keyword != null && !keyword.isBlank() && !containsAny(title, keyword)) {
                continue;
            }
            if (!seen.add(href + "::" + title)) {
                continue;
            }
            String context = extractContext(html, matcher.start(), matcher.end());
            String content = title + " " + cleanText(context);
            items.add(RawNoticeItem.builder()
                    .externalId(Integer.toHexString(Objects.hash(source.getSourceKey(), href, title)))
                    .title(title)
                    .content(content)
                    .originalUrl(href)
                    .category(stringValue(config.get("category")))
                    .publishTime(parseDate(findDate(context)))
                    .rawPayload(Map.of("html", true, "matchedTitle", title))
                    .tags(tagsFromConfig(config))
                    .build());
        }
        return items;
    }

    public List<Map<String, Object>> parseJsonArray(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    public String cleanText(String text) {
        if (text == null) {
            return null;
        }
        String cleaned = SCRIPT_PATTERN.matcher(text).replaceAll(" ");
        cleaned = STYLE_PATTERN.matcher(cleaned).replaceAll(" ");
        cleaned = TAG_PATTERN.matcher(cleaned).replaceAll(" ");
        // Canvas 等页面中可能因为截取上下文导致出现不完整的 "<div ..." 片段，这里做兜底清理。
        cleaned = BROKEN_TAG_PATTERN.matcher(cleaned).replaceAll(" ");
        cleaned = cleaned.replace("&nbsp;", " ").replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">");
        cleaned = SPACE_PATTERN.matcher(cleaned).replaceAll(" ").trim();
        return cleaned.isBlank() ? null : cleaned;
    }

    public LocalDateTime parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ISO_DATE_TIME,
                DateTimeFormatter.RFC_1123_DATE_TIME,
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
                DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                DateTimeFormatter.ofPattern("yyyy/MM/dd"),
                DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm"),
                DateTimeFormatter.ofPattern("yyyy年M月d日"));
        for (DateTimeFormatter formatter : formatters) {
            try {
                if (formatter == DateTimeFormatter.RFC_1123_DATE_TIME) {
                    return ZonedDateTime.parse(value, formatter).toLocalDateTime();
                }
                return LocalDateTime.parse(value, formatter);
            } catch (Exception ignore) {
            }
            try {
                return OffsetDateTime.parse(value, formatter).toLocalDateTime();
            } catch (Exception ignore) {
            }
        }
        Matcher matcher = DATE_PATTERN.matcher(value);
        if (matcher.find()) {
            String normalized = matcher.group(1).replace("年", "-").replace("月", "-").replace("日", "")
                    .replace("/", "-").trim();
            try {
                return normalized.length() > 10
                        ? LocalDateTime.parse(normalized, DateTimeFormatter.ofPattern("yyyy-M-d HH:mm"))
                        : LocalDateTime.parse(normalized + " 00:00", DateTimeFormatter.ofPattern("yyyy-M-d HH:mm"));
            } catch (Exception ignore) {
            }
        }
        return null;
    }

    public String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    public int intValue(Object value, int fallback) {
        if (value == null) {
            return fallback;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            return fallback;
        }
    }

    public String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    @SuppressWarnings("unchecked")
    public Map<String, String> headers(Map<String, Object> config) {
        Object headers = config.get("headers");
        if (!(headers instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<String, String> values = new LinkedHashMap<>();
        map.forEach((key, value) -> {
            if (key != null && value != null) {
                values.put(String.valueOf(key), String.valueOf(value));
            }
        });
        return values;
    }

    @SuppressWarnings("unchecked")
    public List<String> stringList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().filter(Objects::nonNull).map(String::valueOf).toList();
        }
        if (value instanceof String str && !str.isBlank()) {
            return List.of(str);
        }
        return List.of();
    }

    public List<String> tagsFromConfig(Map<String, Object> config) {
        return stringList(config.get("tags"));
    }

    public String findDate(String text) {
        if (text == null) {
            return null;
        }
        Matcher matcher = DATE_PATTERN.matcher(text);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String firstText(Element parent, String tagName) {
        NodeList list = parent.getElementsByTagName(tagName);
        if (list.getLength() == 0) {
            return null;
        }
        Node node = list.item(0);
        return node == null ? null : node.getTextContent();
    }

    private String firstLink(Element parent) {
        String link = firstText(parent, "link");
        if (link != null && !link.isBlank()) {
            return link.trim();
        }
        NodeList list = parent.getElementsByTagName("link");
        for (int i = 0; i < list.getLength(); i++) {
            Node node = list.item(i);
            if (node instanceof Element element && element.hasAttribute("href")) {
                return element.getAttribute("href");
            }
        }
        return null;
    }

    private String extractContext(String html, int start, int end) {
        int from = Math.max(0, start - 120);
        int to = Math.min(html.length(), end + 180);
        return html.substring(from, to);
    }

    private boolean containsAny(String text, String keywordConfig) {
        String lower = Optional.ofNullable(text).orElse("").toLowerCase(Locale.ROOT);
        for (String keyword : keywordConfig.split("[,，\\s]+")) {
            if (!keyword.isBlank() && lower.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String absolutizeUrl(String base, String href) {
        if (href == null || href.isBlank()) {
            return href;
        }
        if (href.startsWith("http://") || href.startsWith("https://")) {
            return href;
        }
        if (base == null || base.isBlank()) {
            return href;
        }
        try {
            return URI.create(base).resolve(href).toString();
        } catch (Exception e) {
            return href;
        }
    }
}
