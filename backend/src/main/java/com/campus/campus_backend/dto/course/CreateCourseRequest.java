package com.campus.campus_backend.dto.course;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateCourseRequest {
    @NotBlank(message = "name required")
    private String name;

    @NotBlank(message = "teacherName required")
    private String teacherName;

    @NotBlank(message = "courseCode required")
    private String courseCode;

    @NotBlank(message = "department required")
    private String department;

    @NotBlank(message = "semester required")
    private String semester;

    @NotBlank(message = "description required")
    private String description;

    private String tags;
}
