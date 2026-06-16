package com.campus.campus_backend.dto.ai;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PostAiSearchRequest {
    @NotBlank(message = "question不能为空")
    private String question;
}
