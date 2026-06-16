package com.campus.campus_backend.dto.course;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateCourseQuestionReplyRequest {
    @NotBlank(message = "content required")
    private String content;
}
