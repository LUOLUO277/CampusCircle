package com.campus.campus_backend.service.ai;

import com.campus.campus_backend.domain.AiSourceType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiRagCandidate {
    private AiSourceType sourceType;
    private Long sourceId;
    private String title;
    private String summary;
    private String content;
    private String route;
    private String publishedAt;
    private double score;
}
