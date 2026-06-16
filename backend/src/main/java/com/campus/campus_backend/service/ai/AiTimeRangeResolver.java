package com.campus.campus_backend.service.ai;

import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;

@Component
public class AiTimeRangeResolver {
    public LocalDateTime resolveStartTime(String question) {
        String q = question == null ? "" : question;
        LocalDateTime now = LocalDateTime.now();
        if (containsAny(q, "本周", "这周", "这星期", "本星期")) {
            return now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).with(LocalTime.MIN);
        }
        if (containsAny(q, "最近", "近期")) {
            return now.minusDays(30);
        }
        return now.minusDays(30);
    }

    private boolean containsAny(String text, String... words) {
        for (String word : words) {
            if (text.contains(word)) {
                return true;
            }
        }
        return false;
    }
}
