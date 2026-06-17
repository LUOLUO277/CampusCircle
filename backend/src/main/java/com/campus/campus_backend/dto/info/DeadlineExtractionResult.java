package com.campus.campus_backend.dto.info;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class DeadlineExtractionResult {
    private final LocalDateTime deadlineAt;
    private final String sourceText;
    private final Double confidence;
}
