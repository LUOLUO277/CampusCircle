package com.campus.campus_backend.dto.ai;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class NoticeAiQueryResponse {
    private String answer;
    private List<MatchedNotice> matchedNotices = new ArrayList<>();

    @Data
    public static class MatchedNotice {
        private Long id;
        private String title;
        private String publishTime;
        private String category;
        private String summary;
        private String reason;
    }
}
