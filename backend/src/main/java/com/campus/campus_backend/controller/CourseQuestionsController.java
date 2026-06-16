package com.campus.campus_backend.controller;

import com.campus.campus_backend.common.Result;
import com.campus.campus_backend.dto.course.CreateCourseQuestionReplyRequest;
import com.campus.campus_backend.service.CourseCircleService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/course-questions")
public class CourseQuestionsController {
    private final CourseCircleService courseCircleService;

    public CourseQuestionsController(CourseCircleService courseCircleService) {
        this.courseCircleService = courseCircleService;
    }

    @GetMapping("/{questionId}")
    public Result<Map<String, Object>> detail(@PathVariable Long questionId) {
        return Result.ok(courseCircleService.getQuestionDetail(questionId));
    }

    @PostMapping("/{questionId}/replies")
    public Result<Map<String, Object>> createReply(@PathVariable Long questionId,
            @Valid @RequestBody CreateCourseQuestionReplyRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        return Result.ok(courseCircleService.createQuestionReply(questionId, request, principal));
    }

    @PatchMapping("/{questionId}/resolved")
    public Result<Map<String, Object>> markResolved(@PathVariable Long questionId,
            @AuthenticationPrincipal UserDetails principal) {
        return Result.ok(courseCircleService.markQuestionResolved(questionId, principal));
    }
}
