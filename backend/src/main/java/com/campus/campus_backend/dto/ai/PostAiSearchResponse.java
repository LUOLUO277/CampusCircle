package com.campus.campus_backend.dto.ai;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PostAiSearchResponse {
    private String answer;
    private List<MatchedPost> matchedPosts = new ArrayList<>();

    @Data
    public static class MatchedPost {
        private Long id;
        private String title;
        private String authorName;
        private String createdAt;
        private String summary;
        private String reason;
    }
}
