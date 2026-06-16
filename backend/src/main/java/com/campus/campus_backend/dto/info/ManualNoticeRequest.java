package com.campus.campus_backend.dto.info;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ManualNoticeRequest {
    private Long sourceId;
    @NotBlank
    @Size(max = 255)
    private String title;
    @Size(max = 500)
    private String summary;
    @Size(max = 50)
    private String category;
    @Size(max = 500)
    private String originalUrl;
    @Size(max = 50)
    private String publishTime;
    @Size(max = 50)
    private String deadline;
    @Size(max = 255)
    private String targetAudience;
    @Size(max = 255)
    private String location;
    @Size(max = 20000)
    private String contentSnapshot;
    private String[] tags;
    private String[] actionLinks;
}
