package com.campus.campus_backend.controller;

import com.campus.campus_backend.common.Result;
import com.campus.campus_backend.service.CourseCircleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/course-experiences")
public class CourseExperiencesController {
    private final CourseCircleService courseCircleService;

    public CourseExperiencesController(CourseCircleService courseCircleService) {
        this.courseCircleService = courseCircleService;
    }

    @GetMapping("/{experienceId}")
    public Result<Map<String, Object>> detail(@PathVariable Long experienceId) {
        return Result.ok(courseCircleService.getExperienceDetail(experienceId));
    }
}
