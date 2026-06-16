package com.campus.campus_backend.dto.course;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateCourseReviewRequest {
    @NotNull(message = "difficultyRating required")
    @Min(value = 1, message = "difficultyRating must be between 1 and 5")
    @Max(value = 5, message = "difficultyRating must be between 1 and 5")
    private Integer difficultyRating;

    @NotNull(message = "workloadRating required")
    @Min(value = 1, message = "workloadRating must be between 1 and 5")
    @Max(value = 5, message = "workloadRating must be between 1 and 5")
    private Integer workloadRating;

    @NotNull(message = "gainRating required")
    @Min(value = 1, message = "gainRating must be between 1 and 5")
    @Max(value = 5, message = "gainRating must be between 1 and 5")
    private Integer gainRating;

    @NotNull(message = "clarityRating required")
    @Min(value = 1, message = "clarityRating must be between 1 and 5")
    @Max(value = 5, message = "clarityRating must be between 1 and 5")
    private Integer clarityRating;

    @NotNull(message = "examPressureRating required")
    @Min(value = 1, message = "examPressureRating must be between 1 and 5")
    @Max(value = 5, message = "examPressureRating must be between 1 and 5")
    private Integer examPressureRating;

    @NotNull(message = "recommendRating required")
    @Min(value = 1, message = "recommendRating must be between 1 and 5")
    @Max(value = 5, message = "recommendRating must be between 1 and 5")
    private Integer recommendRating;

    private String content;
    private String tags;
}
