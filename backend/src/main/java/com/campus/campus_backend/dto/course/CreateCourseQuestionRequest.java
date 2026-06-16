package com.campus.campus_backend.dto.course;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateCourseQuestionRequest {
    @NotBlank(message = "title required")
    private String title;

    @NotBlank(message = "content required")
    private String content;

    @NotBlank(message = "questionType required")
    private String questionType;
}
