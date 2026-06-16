package com.campus.campus_backend.dto.ai;

import lombok.Data;

@Data
public class NoticeCategoryResponse {
    private Long noticeId;
    private String category;
    private String reason;
}
