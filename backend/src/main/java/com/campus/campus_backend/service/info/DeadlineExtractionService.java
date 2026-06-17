package com.campus.campus_backend.service.info;

import com.campus.campus_backend.dto.info.DeadlineExtractionResult;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DeadlineExtractionService {
    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Shanghai");
    private static final List<String> KEYWORDS = List.of(
            "截止时间", "截止至", "截止", "截至", "DDL", "ddl", "提交", "报名", "之前", "前", "申报", "考试", "答辩", "安排");
    private static final List<Pattern> DATE_PATTERNS = List.of(
            Pattern.compile("(?<year>20\\d{2})[-/](?<month>\\d{1,2})[-/](?<day>\\d{1,2})(?:\\s*(?<time>\\d{1,2}:\\d{2}))?"),
            Pattern.compile("(?<year>20\\d{2})年\\s*(?<month>\\d{1,2})月\\s*(?<day>\\d{1,2})日?(?:\\s*(?<time>\\d{1,2}:\\d{2}))?"),
            Pattern.compile("(?<month>\\d{1,2})[-/](?<day>\\d{1,2})(?:\\s*(?<time>\\d{1,2}:\\d{2}))?"),
            Pattern.compile("(?<month>\\d{1,2})月\\s*(?<day>\\d{1,2})日?(?:\\s*(?<time>\\d{1,2}:\\d{2}))?"));

    public DeadlineExtractionResult extract(String title, String summary, String content) {
        List<TextFragment> fragments = List.of(
                new TextFragment("title", defaultText(title), 1.0D),
                new TextFragment("summary", defaultText(summary), 0.9D),
                new TextFragment("content", defaultText(content), 0.85D));

        List<Candidate> candidates = new ArrayList<>();
        for (TextFragment fragment : fragments) {
            if (fragment.text.isBlank()) {
                continue;
            }
            candidates.addAll(findCandidates(fragment));
        }
        if (candidates.isEmpty()) {
            return DeadlineExtractionResult.builder().build();
        }
        candidates.sort(Comparator
                .comparingInt(Candidate::keywordDistance)
                .thenComparingInt(candidate -> candidate.isFuturePreferred() ? 0 : 1)
                .thenComparingInt(Candidate::position));
        Candidate best = candidates.get(0);
        return DeadlineExtractionResult.builder()
                .deadlineAt(best.deadlineAt)
                .sourceText(best.sourceText)
                .confidence(best.confidence)
                .build();
    }

    private List<Candidate> findCandidates(TextFragment fragment) {
        List<Candidate> candidates = new ArrayList<>();
        for (Pattern pattern : DATE_PATTERNS) {
            Matcher matcher = pattern.matcher(fragment.text);
            while (matcher.find()) {
                LocalDateTime parsed = parseCandidate(matcher);
                if (parsed == null) {
                    continue;
                }
                int keywordDistance = nearestKeywordDistance(fragment.text, matcher.start(), matcher.end());
                boolean hasKeyword = keywordDistance < Integer.MAX_VALUE;
                double confidence = fragment.baseConfidence;
                if (hasKeyword) {
                    confidence += 0.12D;
                }
                if (matcher.group("time") != null && !matcher.group("time").isBlank()) {
                    confidence += 0.05D;
                }
                if (matcher.group("year") != null) {
                    confidence += 0.05D;
                }
                confidence = Math.min(confidence, 0.99D);
                candidates.add(new Candidate(parsed, fragment.name + ":" + matcher.group(),
                        clamp(keywordDistance), fragment.text.substring(Math.max(0, matcher.start()),
                                Math.min(fragment.text.length(), matcher.end())),
                        confidence, matcher.start()));
            }
        }
        return candidates;
    }

    private LocalDateTime parseCandidate(Matcher matcher) {
        try {
            int year = matcher.group("year") == null ? Year.now(ZONE_ID).getValue() : Integer.parseInt(matcher.group("year"));
            int month = Integer.parseInt(matcher.group("month"));
            int day = Integer.parseInt(matcher.group("day"));
            int hour = 23;
            int minute = 59;
            String time = matcher.group("time");
            if (time != null && !time.isBlank()) {
                String[] parts = time.split(":");
                hour = Integer.parseInt(parts[0]);
                minute = Integer.parseInt(parts[1]);
            }
            LocalDateTime parsed = LocalDateTime.of(year, month, day, hour, minute);
            if (matcher.group("year") == null) {
                LocalDateTime now = LocalDateTime.now(ZONE_ID);
                if (parsed.isBefore(now.minusDays(180))) {
                    parsed = parsed.plusYears(1);
                }
            }
            return parsed;
        } catch (Exception ignore) {
            return null;
        }
    }

    private int nearestKeywordDistance(String text, int start, int end) {
        int best = Integer.MAX_VALUE;
        String normalized = defaultText(text);
        for (String keyword : KEYWORDS) {
            int from = 0;
            while (from >= 0 && from < normalized.length()) {
                int index = normalized.indexOf(keyword, from);
                if (index < 0) {
                    break;
                }
                int distance = index < start ? start - (index + keyword.length()) : index - end;
                if (distance >= 0) {
                    best = Math.min(best, distance);
                } else if (index <= end && start <= index + keyword.length()) {
                    best = 0;
                }
                from = index + keyword.length();
            }
        }
        return best;
    }

    private int clamp(int keywordDistance) {
        return keywordDistance == Integer.MAX_VALUE ? 10_000 : keywordDistance;
    }

    private String defaultText(String text) {
        return text == null ? "" : text.replace('\n', ' ').replace('\r', ' ').trim();
    }

    private record TextFragment(String name, String text, double baseConfidence) {
    }

    private record Candidate(LocalDateTime deadlineAt, String sourceText, int keywordDistance, String rawMatch,
            double confidence, int position) {
        boolean isFuturePreferred() {
            return deadlineAt == null || !deadlineAt.isBefore(LocalDateTime.now(ZONE_ID).minusDays(1));
        }
    }
}
