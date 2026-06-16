package com.campus.campus_backend.service;

import com.campus.campus_backend.common.BizException;
import com.campus.campus_backend.common.ErrorCode;
import com.campus.campus_backend.domain.Course;
import com.campus.campus_backend.domain.CourseCircleMember;
import com.campus.campus_backend.domain.CourseExperience;
import com.campus.campus_backend.domain.CourseQuestion;
import com.campus.campus_backend.domain.CourseQuestionReply;
import com.campus.campus_backend.domain.CourseReview;
import com.campus.campus_backend.domain.User;
import com.campus.campus_backend.dto.course.CreateCourseRequest;
import com.campus.campus_backend.dto.course.CreateCourseExperienceRequest;
import com.campus.campus_backend.dto.course.CreateCourseQuestionReplyRequest;
import com.campus.campus_backend.dto.course.CreateCourseQuestionRequest;
import com.campus.campus_backend.dto.course.CreateCourseReviewRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import com.campus.campus_backend.repository.CourseCircleMemberRepository;
import com.campus.campus_backend.repository.CourseExperienceRepository;
import com.campus.campus_backend.repository.CourseQuestionReplyRepository;
import com.campus.campus_backend.repository.CourseQuestionRepository;
import com.campus.campus_backend.repository.CourseRepository;
import com.campus.campus_backend.repository.CourseReviewRepository;
import com.campus.campus_backend.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class CourseCircleService {
    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final CourseRepository courseRepository;
    private final CourseCircleMemberRepository memberRepository;
    private final CourseQuestionRepository questionRepository;
    private final CourseQuestionReplyRepository replyRepository;
    private final CourseExperienceRepository experienceRepository;
    private final CourseReviewRepository reviewRepository;
    private final UserRepository userRepository;

    public CourseCircleService(CourseRepository courseRepository,
            CourseCircleMemberRepository memberRepository,
            CourseQuestionRepository questionRepository,
            CourseQuestionReplyRepository replyRepository,
            CourseExperienceRepository experienceRepository,
            CourseReviewRepository reviewRepository,
            UserRepository userRepository) {
        this.courseRepository = courseRepository;
        this.memberRepository = memberRepository;
        this.questionRepository = questionRepository;
        this.replyRepository = replyRepository;
        this.experienceRepository = experienceRepository;
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listCourses(String keyword, UserDetails principal) {
        User currentUser = findCurrentUser(principal);
        return buildCourseCards(courseRepository.search(normalizeKeyword(keyword)), currentUser);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> hotCourses(UserDetails principal) {
        User currentUser = findCurrentUser(principal);
        List<Course> courses = new ArrayList<>(courseRepository.findAll());
        courses.sort((a, b) -> Double.compare(computeHotScore(b), computeHotScore(a)));
        List<Map<String, Object>> list = new ArrayList<>();
        int rank = 1;
        for (Course course : courses.stream().limit(6).toList()) {
            Map<String, Object> item = buildCourseCard(course, currentUser);
            item.put("hotScore", roundNumber(computeHotScore(course)));
            item.put("hotRank", rank++);
            list.add(item);
        }
        return list;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> myCourses(UserDetails principal) {
        User currentUser = findCurrentUser(principal);
        if (currentUser == null) {
            return List.of();
        }
        List<CourseCircleMember> memberships = memberRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId());
        List<Course> courses = memberships.stream()
                .map(CourseCircleMember::getCourse)
                .filter(Objects::nonNull)
                .toList();
        return buildCourseCards(courses, currentUser);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getCourseDetail(Long courseId, UserDetails principal) {
        Course course = getCourseOrThrow(courseId);
        User currentUser = findCurrentUser(principal);
        Map<String, Object> data = buildCourseCard(course, currentUser);
        data.put("description", course.getDescription());
        data.put("reviewSummary", getCourseReviewSummary(courseId));
        data.put("latestQuestions", listCourseQuestions(courseId, null, null, "latest", 1, 5));
        data.put("latestExperiences", listCourseExperiences(courseId, null, null, "latest", 1, 5));
        data.put("latestReviews", listCourseReviews(courseId));
        return data;
    }

    @Transactional
    public Map<String, Object> createCourse(CreateCourseRequest request, UserDetails principal) {
        User currentUser = requireCurrentUser(principal);
        if (courseRepository.findByCourseCodeIgnoreCase(request.getCourseCode().trim()).isPresent()) {
            throw new BizException(ErrorCode.CONFLICT.getCode(), "course already exists");
        }

        Course course = new Course();
        course.setName(request.getName().trim());
        course.setTeacherName(request.getTeacherName().trim());
        course.setCourseCode(request.getCourseCode().trim());
        course.setDepartment(request.getDepartment().trim());
        course.setSemester(request.getSemester().trim());
        course.setDescription(request.getDescription().trim());
        course.setTags(trimNullable(request.getTags()));
        course = courseRepository.save(course);

        CourseCircleMember member = new CourseCircleMember();
        member.setCourse(course);
        member.setUser(currentUser);
        memberRepository.save(member);
        refreshCourseCounts(course);
        return buildCourseCard(course, currentUser);
    }

    @Transactional
    public Map<String, Object> joinCourse(Long courseId, UserDetails principal) {
        User currentUser = requireCurrentUser(principal);
        Course course = getCourseOrThrow(courseId);
        if (!memberRepository.existsByCourseIdAndUserId(courseId, currentUser.getId())) {
            CourseCircleMember member = new CourseCircleMember();
            member.setCourse(course);
            member.setUser(currentUser);
            memberRepository.save(member);
            refreshCourseCounts(course);
        }
        return buildMembershipState(course, true);
    }

    @Transactional
    public Map<String, Object> quitCourse(Long courseId, UserDetails principal) {
        User currentUser = requireCurrentUser(principal);
        Course course = getCourseOrThrow(courseId);
        memberRepository.findByCourseIdAndUserId(courseId, currentUser.getId()).ifPresent(memberRepository::delete);
        refreshCourseCounts(course);
        return buildMembershipState(course, false);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listCourseQuestions(Long courseId, String keyword, Boolean resolved, String sort,
            int page, int pageSize) {
        getCourseOrThrow(courseId);
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), normalizePageSize(pageSize), questionSort(sort));
        Page<CourseQuestion> questionPage = questionRepository.searchPage(courseId, normalizeKeyword(keyword), resolved,
                pageable);
        List<Map<String, Object>> list = questionPage.getContent().stream()
                .map(this::buildQuestionItem)
                .toList();
        return buildPageData(list, questionPage);
    }

    @Transactional
    public Map<String, Object> createQuestion(Long courseId, CreateCourseQuestionRequest request, UserDetails principal) {
        User currentUser = requireCurrentUser(principal);
        Course course = getCourseOrThrow(courseId);

        CourseQuestion question = new CourseQuestion();
        question.setCourse(course);
        question.setUser(currentUser);
        question.setTitle(request.getTitle().trim());
        question.setContent(request.getContent().trim());
        question.setQuestionType(request.getQuestionType().trim());
        question = questionRepository.save(question);

        refreshCourseCounts(course);
        return buildQuestionDetail(question);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getQuestionDetail(Long questionId) {
        CourseQuestion question = getQuestionOrThrow(questionId);
        return buildQuestionDetail(question);
    }

    @Transactional
    public Map<String, Object> createQuestionReply(Long questionId, CreateCourseQuestionReplyRequest request,
            UserDetails principal) {
        User currentUser = requireCurrentUser(principal);
        CourseQuestion question = getQuestionOrThrow(questionId);

        CourseQuestionReply reply = new CourseQuestionReply();
        reply.setQuestion(question);
        reply.setUser(currentUser);
        reply.setContent(request.getContent().trim());
        reply = replyRepository.save(reply);

        question.setReplyCount((int) replyRepository.countByQuestionId(questionId));
        questionRepository.save(question);

        return buildQuestionReply(reply);
    }

    @Transactional
    public Map<String, Object> markQuestionResolved(Long questionId, UserDetails principal) {
        User currentUser = requireCurrentUser(principal);
        CourseQuestion question = getQuestionOrThrow(questionId);
        boolean isAdmin = "ADMIN".equalsIgnoreCase(currentUser.getRole());
        if (!isAdmin && !question.getUser().getId().equals(currentUser.getId())) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        question.setResolved(true);
        questionRepository.save(question);
        return buildQuestionDetail(question);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listCourseExperiences(Long courseId, String keyword, String semester, String sort,
            int page, int pageSize) {
        getCourseOrThrow(courseId);
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), normalizePageSize(pageSize), experienceSort(sort));
        Page<CourseExperience> experiencePage = experienceRepository.searchPage(courseId, normalizeKeyword(keyword),
                trimNullable(semester), pageable);
        List<Map<String, Object>> list = experiencePage.getContent().stream()
                .map(this::buildExperiencePreviewItem)
                .toList();
        return buildPageData(list, experiencePage);
    }

    @Transactional
    public Map<String, Object> createExperience(Long courseId, CreateCourseExperienceRequest request,
            UserDetails principal) {
        User currentUser = requireCurrentUser(principal);
        Course course = getCourseOrThrow(courseId);

        CourseExperience experience = new CourseExperience();
        experience.setCourse(course);
        experience.setUser(currentUser);
        experience.setSemester(request.getSemester().trim());
        experience.setTeacherName(request.getTeacherName().trim());
        experience.setDifficultyRating(request.getDifficultyRating());
        experience.setWorkloadRating(request.getWorkloadRating());
        experience.setGainRating(request.getGainRating());
        experience.setExamPressureRating(request.getExamPressureRating());
        experience.setRecommendRating(request.getRecommendRating());
        experience.setStudyAdvice(request.getStudyAdvice().trim());
        experience.setPitfallAdvice(request.getPitfallAdvice().trim());
        experience.setSuitableFor(request.getSuitableFor().trim());
        experience = experienceRepository.save(experience);

        refreshCourseCounts(course);
        return buildExperienceItem(experience);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getExperienceDetail(Long experienceId) {
        CourseExperience experience = experienceRepository.findById(experienceId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));
        return buildExperienceItem(experience);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listCourseReviews(Long courseId) {
        getCourseOrThrow(courseId);
        return reviewRepository.findByCourseIdOrderByCreatedAtDesc(courseId).stream()
                .map(this::buildReviewItem)
                .toList();
    }

    @Transactional
    public Map<String, Object> createOrUpdateReview(Long courseId, CreateCourseReviewRequest request,
            UserDetails principal) {
        User currentUser = requireCurrentUser(principal);
        Course course = getCourseOrThrow(courseId);

        CourseReview review = reviewRepository.findByCourseIdAndUserId(courseId, currentUser.getId())
                .orElseGet(CourseReview::new);
        review.setCourse(course);
        review.setUser(currentUser);
        review.setDifficultyRating(request.getDifficultyRating());
        review.setWorkloadRating(request.getWorkloadRating());
        review.setGainRating(request.getGainRating());
        review.setClarityRating(request.getClarityRating());
        review.setExamPressureRating(request.getExamPressureRating());
        review.setRecommendRating(request.getRecommendRating());
        review.setContent(trimNullable(request.getContent()));
        review.setTags(trimNullable(request.getTags()));
        review = reviewRepository.save(review);

        refreshCourseCounts(course);
        return buildReviewItem(review);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getCourseReviewSummary(Long courseId) {
        getCourseOrThrow(courseId);
        Object[] summary = normalizeSummary(reviewRepository.summarizeByCourseId(courseId));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("averageDifficulty", roundNumber(summary[0]));
        data.put("averageWorkload", roundNumber(summary[1]));
        data.put("averageGain", roundNumber(summary[2]));
        data.put("averageClarity", roundNumber(summary[3]));
        data.put("averageExamPressure", roundNumber(summary[4]));
        data.put("averageRecommend", roundNumber(summary[5]));
        data.put("reviewCount", ((Number) summary[6]).longValue());
        return data;
    }

    private List<Map<String, Object>> buildCourseCards(List<Course> courses, User currentUser) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Course course : courses) {
            list.add(buildCourseCard(course, currentUser));
        }
        return list;
    }

    private Map<String, Object> buildCourseCard(Course course, User currentUser) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", course.getId());
        item.put("name", course.getName());
        item.put("teacherName", course.getTeacherName());
        item.put("courseCode", course.getCourseCode());
        item.put("department", course.getDepartment());
        item.put("semester", course.getSemester());
        item.put("tags", splitTags(course.getTags()));
        item.put("memberCount", safeCount(course.getMemberCount()));
        item.put("questionCount", safeCount(course.getQuestionCount()));
        item.put("experienceCount", safeCount(course.getExperienceCount()));
        item.put("reviewCount", safeCount(course.getReviewCount()));
        item.put("isJoined", currentUser != null
                && memberRepository.existsByCourseIdAndUserId(course.getId(), currentUser.getId()));
        return item;
    }

    private Map<String, Object> buildQuestionItem(CourseQuestion question) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", question.getId());
        item.put("courseId", question.getCourse().getId());
        item.put("title", question.getTitle());
        item.put("content", question.getContent());
        item.put("questionType", question.getQuestionType());
        item.put("resolved", Boolean.TRUE.equals(question.getResolved()));
        item.put("replyCount", safeCount(question.getReplyCount()));
        item.put("createdAt", formatTime(question.getCreatedAt()));
        item.put("updatedAt", formatTime(question.getUpdatedAt()));
        item.put("author", buildAuthor(question.getUser()));
        return item;
    }

    private Map<String, Object> buildQuestionDetail(CourseQuestion question) {
        Map<String, Object> data = buildQuestionItem(question);
        data.put("course", buildCourseCard(question.getCourse(), null));
        data.put("replies", replyRepository.findByQuestionIdOrderByCreatedAtAsc(question.getId()).stream()
                .map(this::buildQuestionReply)
                .toList());
        return data;
    }

    private Map<String, Object> buildQuestionReply(CourseQuestionReply reply) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", reply.getId());
        item.put("questionId", reply.getQuestion().getId());
        item.put("content", reply.getContent());
        item.put("createdAt", formatTime(reply.getCreatedAt()));
        item.put("author", buildAuthor(reply.getUser()));
        return item;
    }

    private Map<String, Object> buildExperienceItem(CourseExperience experience) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", experience.getId());
        item.put("courseId", experience.getCourse().getId());
        item.put("courseName", experience.getCourse().getName());
        item.put("semester", experience.getSemester());
        item.put("teacherName", experience.getTeacherName());
        item.put("difficultyRating", experience.getDifficultyRating());
        item.put("workloadRating", experience.getWorkloadRating());
        item.put("gainRating", experience.getGainRating());
        item.put("examPressureRating", experience.getExamPressureRating());
        item.put("recommendRating", experience.getRecommendRating());
        item.put("studyAdvice", experience.getStudyAdvice());
        item.put("pitfallAdvice", experience.getPitfallAdvice());
        item.put("suitableFor", experience.getSuitableFor());
        item.put("createdAt", formatTime(experience.getCreatedAt()));
        item.put("updatedAt", formatTime(experience.getUpdatedAt()));
        item.put("author", buildAuthor(experience.getUser()));
        return item;
    }

    private Map<String, Object> buildExperiencePreviewItem(CourseExperience experience) {
        Map<String, Object> item = buildExperienceItem(experience);
        item.put("previewStudyAdvice", abbreviate(experience.getStudyAdvice(), 80));
        item.put("previewPitfallAdvice", abbreviate(experience.getPitfallAdvice(), 80));
        item.put("previewSuitableFor", abbreviate(experience.getSuitableFor(), 60));
        return item;
    }

    private Map<String, Object> buildReviewItem(CourseReview review) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", review.getId());
        item.put("courseId", review.getCourse().getId());
        item.put("difficultyRating", review.getDifficultyRating());
        item.put("workloadRating", review.getWorkloadRating());
        item.put("gainRating", review.getGainRating());
        item.put("clarityRating", review.getClarityRating());
        item.put("examPressureRating", review.getExamPressureRating());
        item.put("recommendRating", review.getRecommendRating());
        item.put("content", review.getContent());
        item.put("tags", splitTags(review.getTags()));
        item.put("createdAt", formatTime(review.getCreatedAt()));
        item.put("author", buildAuthor(review.getUser()));
        return item;
    }

    private Map<String, Object> buildMembershipState(Course course, boolean isJoined) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("courseId", course.getId());
        data.put("isJoined", isJoined);
        data.put("memberCount", safeCount(course.getMemberCount()));
        return data;
    }

    private Map<String, Object> buildAuthor(User user) {
        Map<String, Object> author = new LinkedHashMap<>();
        author.put("id", user.getId());
        author.put("name", user.getNickname() != null && !user.getNickname().isBlank()
                ? user.getNickname()
                : user.getUsername());
        author.put("avatarUrl", user.getAvatarUrl());
        return author;
    }

    private void refreshCourseCounts(Course course) {
        course.setMemberCount((int) memberRepository.countByCourseId(course.getId()));
        course.setQuestionCount((int) questionRepository.countByCourseId(course.getId()));
        course.setExperienceCount((int) experienceRepository.countByCourseId(course.getId()));
        course.setReviewCount((int) reviewRepository.countByCourseId(course.getId()));
        courseRepository.save(course);
    }

    private Course getCourseOrThrow(Long courseId) {
        return courseRepository.findById(courseId).orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));
    }

    private CourseQuestion getQuestionOrThrow(Long questionId) {
        return questionRepository.findById(questionId).orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));
    }

    private User requireCurrentUser(UserDetails principal) {
        User currentUser = findCurrentUser(principal);
        if (currentUser == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return currentUser;
    }

    private User findCurrentUser(UserDetails principal) {
        if (principal == null) {
            return null;
        }
        return userRepository.findByUsername(principal.getUsername()).orElse(null);
    }

    private String formatTime(java.time.LocalDateTime time) {
        if (time == null) {
            return "";
        }
        return time.atZone(ZONE_ID).format(DATE_TIME_FORMATTER);
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String trimmed = keyword.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private int normalizePageSize(int pageSize) {
        if (pageSize <= 0) {
            return 10;
        }
        return Math.min(pageSize, 50);
    }

    private List<String> splitTags(String tags) {
        if (tags == null || tags.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private int safeCount(Integer value) {
        return value == null ? 0 : value;
    }

    private double roundNumber(Object value) {
        if (!(value instanceof Number number)) {
            return 0D;
        }
        return Math.round(number.doubleValue() * 10D) / 10D;
    }

    private Object[] normalizeSummary(Object[] summary) {
        if (summary == null) {
            return new Object[] { 0D, 0D, 0D, 0D, 0D, 0D, 0L };
        }
        if (summary.length == 1 && summary[0] instanceof Object[] nested) {
            return normalizeSummary(nested);
        }
        if (summary.length >= 7) {
            return summary;
        }
        Object[] normalized = new Object[] { 0D, 0D, 0D, 0D, 0D, 0D, 0L };
        for (int i = 0; i < summary.length && i < normalized.length; i++) {
            normalized[i] = summary[i];
        }
        return normalized;
    }

    private String trimNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Sort questionSort(String sort) {
        if ("oldest".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Direction.ASC, "createdAt");
        }
        if ("unresolved".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Order.asc("resolved"), Sort.Order.desc("createdAt"));
        }
        return Sort.by(Sort.Direction.DESC, "createdAt");
    }

    private Sort experienceSort(String sort) {
        if ("recommend".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Order.desc("recommendRating"), Sort.Order.desc("createdAt"));
        }
        if ("gain".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Order.desc("gainRating"), Sort.Order.desc("createdAt"));
        }
        if ("difficulty".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Order.desc("difficultyRating"), Sort.Order.desc("createdAt"));
        }
        return Sort.by(Sort.Direction.DESC, "createdAt");
    }

    private Map<String, Object> buildPageData(List<Map<String, Object>> list, Page<?> pageData) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("list", list);
        data.put("total", pageData.getTotalElements());
        data.put("page", pageData.getNumber() + 1);
        data.put("pageSize", pageData.getSize());
        data.put("hasMore", pageData.hasNext());
        return data;
    }

    private double computeHotScore(Course course) {
        double score = safeCount(course.getMemberCount()) * 1.2
                + safeCount(course.getQuestionCount()) * 2.8
                + safeCount(course.getExperienceCount()) * 2.2
                + safeCount(course.getReviewCount()) * 2.5;
        if (course.getUpdatedAt() != null) {
            long days = java.time.Duration.between(course.getUpdatedAt(), java.time.LocalDateTime.now()).toDays();
            score += Math.max(0, 30 - Math.min(days, 30));
        }
        return score;
    }

    private String abbreviate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
}
