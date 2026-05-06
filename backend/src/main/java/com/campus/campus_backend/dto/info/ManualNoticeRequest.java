package com.campus.campus_backend.dto.info;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ManualNoticeRequest {
    private Long sourceId;
    @NotBlank
    private String title;
    private String summary;
    private String category;
    private String originalUrl;
    private String publishTime;
    private String deadline;
    private String targetAudience;
    private String location;
    private String contentSnapshot;
    private String[] tags;
    private String[] actionLinks;
}
