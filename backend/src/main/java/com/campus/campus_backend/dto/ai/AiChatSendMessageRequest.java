package com.campus.campus_backend.dto.ai;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiChatSendMessageRequest {
    @NotBlank
    private String content;
}
