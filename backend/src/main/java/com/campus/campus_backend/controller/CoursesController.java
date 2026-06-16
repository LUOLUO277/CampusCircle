package com.campus.campus_backend.controller;

import com.campus.campus_backend.common.Result;
import com.campus.campus_backend.dto.course.CreateCourseRequest;
import com.campus.campus_backend.dto.course.CreateCourseExperienceRequest;
import com.campus.campus_backend.dto.course.CreateCourseQuestionRequest;
import com.campus.campus_backend.dto.course.CreateCourseReviewRequest;
import com.campus.campus_backend.service.CourseCircleService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/courses")
public class CoursesController {
    private final CourseCircleService courseCircleService;

    public CoursesController(CourseCircleService courseCircleService) {
        this.courseCircleService = courseCircleService;
    }

    @GetMapping
    public Result<List<Map<String, Object>>> list(@RequestParam(required = false) String keyword,
            @AuthenticationPrincipal UserDetails principal) {
        return Result.ok(courseCircleService.listCourses(keyword, principal));
    }

    @PostMapping
    public Result<Map<String, Object>> create(@Valid @RequestBody CreateCourseRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        return Result.ok(courseCircleService.createCourse(request, principal));
    }

    @GetMapping("/search")
    public Result<List<Map<String, Object>>> search(@RequestParam(required = false) String keyword,
            @AuthenticationPrincipal UserDetails principal) {
        return Result.ok(courseCircleService.listCourses(keyword, principal));
    }

    @GetMapping("/hot")
    public Result<List<Map<String, Object>>> hot(@AuthenticationPrincipal UserDetails principal) {
        return Result.ok(courseCircleService.hotCourses(principal));
    }

    @GetMapping("/mine")
    public Result<List<Map<String, Object>>> mine(@AuthenticationPrincipal UserDetails principal) {
        return Result.ok(courseCircleService.myCourses(principal));
    }

    @GetMapping("/{courseId}")
    public Result<Map<String, Object>> detail(@PathVariable Long courseId,
            @AuthenticationPrincipal UserDetails principal) {
        return Result.ok(courseCircleService.getCourseDetail(courseId, principal));
    }

    @PostMapping("/{courseId}/join")
    public Result<Map<String, Object>> join(@PathVariable Long courseId,
            @AuthenticationPrincipal UserDetails principal) {
        return Result.ok(courseCircleService.joinCourse(courseId, principal));
    }

    @DeleteMapping("/{courseId}/join")
    public Result<Map<String, Object>> quit(@PathVariable Long courseId,
            @AuthenticationPrincipal UserDetails principal) {
        return Result.ok(courseCircleService.quitCourse(courseId, principal));
    }

    @GetMapping("/{courseId}/questions")
    public Result<Map<String, Object>> questions(@PathVariable Long courseId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean resolved,
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(courseCircleService.listCourseQuestions(courseId, keyword, resolved, sort, page, pageSize));
    }

    @PostMapping("/{courseId}/questions")
    public Result<Map<String, Object>> createQuestion(@PathVariable Long courseId,
            @Valid @RequestBody CreateCourseQuestionRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        return Result.ok(courseCircleService.createQuestion(courseId, request, principal));
    }

    @GetMapping("/{courseId}/experiences")
    public Result<Map<String, Object>> experiences(@PathVariable Long courseId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String semester,
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(courseCircleService.listCourseExperiences(courseId, keyword, semester, sort, page, pageSize));
    }

    @PostMapping("/{courseId}/experiences")
    public Result<Map<String, Object>> createExperience(@PathVariable Long courseId,
            @Valid @RequestBody CreateCourseExperienceRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        return Result.ok(courseCircleService.createExperience(courseId, request, principal));
    }

    @GetMapping("/{courseId}/reviews")
    public Result<List<Map<String, Object>>> reviews(@PathVariable Long courseId) {
        return Result.ok(courseCircleService.listCourseReviews(courseId));
    }

    @PostMapping("/{courseId}/reviews")
    public Result<Map<String, Object>> createReview(@PathVariable Long courseId,
            @Valid @RequestBody CreateCourseReviewRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        return Result.ok(courseCircleService.createOrUpdateReview(courseId, request, principal));
    }

    @GetMapping("/{courseId}/review-summary")
    public Result<Map<String, Object>> reviewSummary(@PathVariable Long courseId) {
        return Result.ok(courseCircleService.getCourseReviewSummary(courseId));
    }
}
