package com.campus.campus_backend.dto.info;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateNoticeCommentRequest {
    private Long parentId;
    private Long replyToCommentId;
    @NotBlank
    private String content;
    private Boolean anonymous;
}
