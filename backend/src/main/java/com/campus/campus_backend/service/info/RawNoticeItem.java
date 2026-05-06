package com.campus.campus_backend.service.info;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Builder
public class RawNoticeItem {
    private String externalId;
    private String title;
    private String content;
    private String originalUrl;
    private String category;
    private LocalDateTime publishTime;
    private Map<String, Object> rawPayload;
    private List<String> tags;
}
