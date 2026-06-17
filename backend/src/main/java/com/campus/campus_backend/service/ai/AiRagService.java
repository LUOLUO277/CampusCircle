package com.campus.campus_backend.service.ai;

import com.campus.campus_backend.domain.*;
import com.campus.campus_backend.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AiRagService {
    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final Pattern ASCII_TOKEN = Pattern.compile("[a-zA-Z0-9_+#.-]{2,}");
    private static final Pattern CHINESE_TOKEN = Pattern.compile("[\\u4e00-\\u9fa5]{2,}");
    private static final Set<String> STOP_WORDS = Set.of(
            "最近", "什么", "哪些", "一下", "一个", "一下子", "帮我", "相关", "内容", "信息", "有没有",
            "可以", "一下下", "这个", "那个", "怎么", "如何", "一下吧", "大家", "里面", "校园", "帮忙",
            "总结", "查询", "查找", "看看", "一下呢", "请问");
    private static final Set<String> NOTICE_HINTS = Set.of("通知", "作业", "截止", "报名", "讲座", "安排", "考试", "ddl", "deadline");
    private static final Set<String> POST_HINTS = Set.of("帖子", "讨论", "求助", "闲置", "投票", "社区", "热帖", "发帖", "有人说");
    private static final Set<String> COURSE_HINTS = Set.of("课程", "老师", "作业量", "难度", "经验", "复习", "课", "数据库", "数据结构", "canvas");

    private final AggregatedNoticeRepository noticeRepository;
    private final PostRepository postRepository;
    private final CourseRepository courseRepository;
    private final CourseQuestionRepository courseQuestionRepository;
    private final CourseExperienceRepository courseExperienceRepository;

    public AiRagService(AggregatedNoticeRepository noticeRepository,
                        PostRepository postRepository,
                        CourseRepository courseRepository,
                        CourseQuestionRepository courseQuestionRepository,
                        CourseExperienceRepository courseExperienceRepository) {
        this.noticeRepository = noticeRepository;
        this.postRepository = postRepository;
        this.courseRepository = courseRepository;
        this.courseQuestionRepository = courseQuestionRepository;
        this.courseExperienceRepository = courseExperienceRepository;
    }

    @Transactional(readOnly = true)
    public List<AiRagCandidate> retrieve(String query) {
        EnumSet<AiSourceType> scopes = resolveScopes(query);
        List<String> tokens = tokenize(query);
        List<AiRagCandidate> candidates = new ArrayList<>();

        if (scopes.contains(AiSourceType.NOTICE)) {
            List<AggregatedNotice> notices = noticeRepository.findRecentForAi(
                    "ONLINE",
                    LocalDateTime.now().minusDays(90),
                    PageRequest.of(0, 60)
            );
            for (AggregatedNotice notice : notices) {
                AiRagCandidate candidate = buildNoticeCandidate(notice, query, tokens);
                if (candidate.getScore() > 0.5D) {
                    candidates.add(candidate);
                }
            }
        }

        if (scopes.contains(AiSourceType.POST)) {
            List<Post> posts = postRepository.findRecentForAi(LocalDateTime.now().minusDays(90), PageRequest.of(0, 60));
            for (Post post : posts) {
                AiRagCandidate candidate = buildPostCandidate(post, query, tokens);
                if (candidate.getScore() > 0.5D) {
                    candidates.add(candidate);
                }
            }
        }

        if (scopes.contains(AiSourceType.COURSE)) {
            List<Course> courses = courseRepository.findAll(PageRequest.of(0, 40, Sort.by(Sort.Direction.DESC, "updatedAt"))).getContent();
            for (Course course : courses) {
                AiRagCandidate candidate = buildCourseCandidate(course, query, tokens);
                if (candidate.getScore() > 0.5D) {
                    candidates.add(candidate);
                }
            }

            List<CourseQuestion> questions = courseQuestionRepository.findAll(PageRequest.of(0, 40, Sort.by(Sort.Direction.DESC, "createdAt"))).getContent();
            for (CourseQuestion question : questions) {
                AiRagCandidate candidate = buildCourseQuestionCandidate(question, query, tokens);
                if (candidate.getScore() > 0.5D) {
                    candidates.add(candidate);
                }
            }

            List<CourseExperience> experiences = courseExperienceRepository.findAll(PageRequest.of(0, 40, Sort.by(Sort.Direction.DESC, "createdAt"))).getContent();
            for (CourseExperience experience : experiences) {
                AiRagCandidate candidate = buildCourseExperienceCandidate(experience, query, tokens);
                if (candidate.getScore() > 0.5D) {
                    candidates.add(candidate);
                }
            }
        }

        candidates.sort(Comparator.comparingDouble(AiRagCandidate::getScore).reversed());
        if (candidates.size() > 8) {
            return new ArrayList<>(candidates.subList(0, 8));
        }
        return candidates;
    }

    public Map<String, Object> buildDebugPayload(String query) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("query", query);
        data.put("scopes", resolveScopes(query).stream().map(Enum::name).toList());
        data.put("tokens", tokenize(query));
        data.put("candidates", retrieve(query).stream().map(this::toMap).toList());
        return data;
    }

    private AiRagCandidate buildNoticeCandidate(AggregatedNotice notice, String query, List<String> tokens) {
        String summary = firstNonBlank(notice.getSummary(), notice.getContentSnapshot());
        double score = scoreText(query, tokens, notice.getTitle(), summary, notice.getContentSnapshot(),
                notice.getCategory(), notice.getSourceName());
        score += recencyBonus(notice.getPublishTime(), 12D);
        if (containsAny(query, NOTICE_HINTS)) {
            score += 1.5D;
        }
        AiRagCandidate candidate = new AiRagCandidate();
        candidate.setSourceType(AiSourceType.NOTICE);
        candidate.setSourceId(notice.getId());
        candidate.setTitle(notice.getTitle());
        candidate.setSummary(abbreviate(summary, 140));
        candidate.setContent(abbreviate(notice.getContentSnapshot(), 600));
        candidate.setRoute("/pages/info-center/detail?id=" + notice.getId());
        candidate.setPublishedAt(formatTime(notice.getPublishTime()));
        candidate.setScore(score);
        return candidate;
    }

    private AiRagCandidate buildPostCandidate(Post post, String query, List<String> tokens) {
        String title = firstNonBlank(post.getSummary(), abbreviate(post.getContent(), 24));
        String authorName = post.getUser() == null ? "" : firstNonBlank(post.getUser().getNickname(), post.getUser().getUsername());
        double score = scoreText(query, tokens, title, post.getSummary(), post.getContent(),
                post.getTags(), post.getCategory() == null ? "" : post.getCategory().getName(), authorName);
        score += recencyBonus(post.getCreatedAt(), 8D);
        score += Math.min(safeInt(post.getHotScore()) / 50D, 6D);
        score += Math.min((safeInt(post.getLikeCount()) + safeInt(post.getCommentCount())) / 20D, 4D);
        if (containsAny(query, POST_HINTS)) {
            score += 1.5D;
        }
        AiRagCandidate candidate = new AiRagCandidate();
        candidate.setSourceType(AiSourceType.POST);
        candidate.setSourceId(post.getId());
        candidate.setTitle(title);
        candidate.setSummary(abbreviate(post.getContent(), 140));
        candidate.setContent(abbreviate(post.getContent(), 600));
        candidate.setRoute("/pages/post/detail?id=" + post.getId());
        candidate.setPublishedAt(formatTime(post.getCreatedAt()));
        candidate.setScore(score);
        return candidate;
    }

    private AiRagCandidate buildCourseCandidate(Course course, String query, List<String> tokens) {
        String summary = course.getTeacherName() + " | " + course.getDepartment() + " | " + firstNonBlank(course.getSemester(), "");
        double score = scoreText(query, tokens, course.getName(), summary, course.getDescription(),
                course.getCourseCode(), course.getTeacherName(), course.getTags());
        score += recencyBonus(course.getUpdatedAt(), 8D);
        score += Math.min((safeInt(course.getMemberCount()) + safeInt(course.getQuestionCount()) * 2 + safeInt(course.getExperienceCount()) * 2) / 12D, 8D);
        if (containsAny(query, COURSE_HINTS)) {
            score += 1.5D;
        }
        AiRagCandidate candidate = new AiRagCandidate();
        candidate.setSourceType(AiSourceType.COURSE);
        candidate.setSourceId(course.getId());
        candidate.setTitle(course.getName());
        candidate.setSummary(abbreviate(course.getDescription(), 140));
        candidate.setContent(abbreviate(course.getDescription(), 600));
        candidate.setRoute("/pages/course-circle/detail?id=" + course.getId());
        candidate.setPublishedAt(formatTime(course.getUpdatedAt()));
        candidate.setScore(score);
        return candidate;
    }

    private AiRagCandidate buildCourseQuestionCandidate(CourseQuestion question, String query, List<String> tokens) {
        String courseName = question.getCourse() == null ? "" : question.getCourse().getName();
        double score = scoreText(query, tokens, question.getTitle(), question.getContent(), courseName,
                question.getQuestionType());
        score += recencyBonus(question.getCreatedAt(), 7D);
        score += Math.min(safeInt(question.getReplyCount()) / 3D, 4D);
        if (Boolean.FALSE.equals(question.getResolved())) {
            score += 0.6D;
        }
        if (containsAny(query, COURSE_HINTS)) {
            score += 1.2D;
        }
        AiRagCandidate candidate = new AiRagCandidate();
        candidate.setSourceType(AiSourceType.COURSE_QUESTION);
        candidate.setSourceId(question.getId());
        candidate.setTitle(question.getTitle());
        candidate.setSummary(abbreviate(courseName + " | " + question.getContent(), 140));
        candidate.setContent(abbreviate(question.getContent(), 600));
        candidate.setRoute("/pages/course-circle/question-detail?id=" + question.getId());
        candidate.setPublishedAt(formatTime(question.getCreatedAt()));
        candidate.setScore(score);
        return candidate;
    }

    private AiRagCandidate buildCourseExperienceCandidate(CourseExperience experience, String query, List<String> tokens) {
        String courseName = experience.getCourse() == null ? "" : experience.getCourse().getName();
        String summary = firstNonBlank(experience.getStudyAdvice(), experience.getPitfallAdvice(), experience.getSuitableFor());
        double score = scoreText(query, tokens, courseName, summary, experience.getStudyAdvice(),
                experience.getPitfallAdvice(), experience.getSuitableFor(), experience.getTeacherName(), experience.getSemester());
        score += recencyBonus(experience.getCreatedAt(), 7D);
        score += Math.min((safeInt(experience.getRecommendRating()) + safeInt(experience.getGainRating())) / 2D, 5D);
        if (containsAny(query, COURSE_HINTS)) {
            score += 1.2D;
        }
        AiRagCandidate candidate = new AiRagCandidate();
        candidate.setSourceType(AiSourceType.COURSE_EXPERIENCE);
        candidate.setSourceId(experience.getId());
        candidate.setTitle(courseName + " 经验分享");
        candidate.setSummary(abbreviate(summary, 140));
        candidate.setContent(abbreviate(String.join("\n", List.of(
                defaultText(experience.getStudyAdvice()),
                defaultText(experience.getPitfallAdvice()),
                defaultText(experience.getSuitableFor())
        )), 600));
        candidate.setRoute("/pages/course-circle/experience-detail?id=" + experience.getId());
        candidate.setPublishedAt(formatTime(experience.getCreatedAt()));
        candidate.setScore(score);
        return candidate;
    }

    private EnumSet<AiSourceType> resolveScopes(String query) {
        boolean notice = containsAny(query, NOTICE_HINTS);
        boolean post = containsAny(query, POST_HINTS);
        boolean course = containsAny(query, COURSE_HINTS);

        EnumSet<AiSourceType> scopes = EnumSet.noneOf(AiSourceType.class);
        if (notice) {
            scopes.add(AiSourceType.NOTICE);
        }
        if (post) {
            scopes.add(AiSourceType.POST);
        }
        if (course) {
            scopes.add(AiSourceType.COURSE);
        }
        if (scopes.isEmpty()) {
            scopes.add(AiSourceType.NOTICE);
            scopes.add(AiSourceType.POST);
            scopes.add(AiSourceType.COURSE);
        }
        return scopes;
    }

    private double scoreText(String query, List<String> tokens, String title, String summary, String content, String... metadata) {
        String normalizedQuery = normalize(query);
        String normalizedTitle = normalize(title);
        String normalizedSummary = normalize(summary);
        String normalizedContent = normalize(content);
        String normalizedMeta = normalize(String.join(" ", Arrays.stream(metadata).filter(Objects::nonNull).toList()));

        double score = 0D;
        if (!normalizedQuery.isBlank()) {
            if (normalizedTitle.contains(normalizedQuery)) {
                score += 24D;
            }
            if (normalizedSummary.contains(normalizedQuery)) {
                score += 16D;
            }
            if (normalizedContent.contains(normalizedQuery)) {
                score += 12D;
            }
            if (normalizedMeta.contains(normalizedQuery)) {
                score += 8D;
            }
        }

        Set<String> dedup = new LinkedHashSet<>(tokens);
        for (String token : dedup) {
            String normalizedToken = normalize(token);
            if (normalizedToken.length() < 2) {
                continue;
            }
            if (normalizedTitle.contains(normalizedToken)) {
                score += 7D;
            }
            if (normalizedSummary.contains(normalizedToken)) {
                score += 4D;
            }
            if (normalizedContent.contains(normalizedToken)) {
                score += 2.5D;
            }
            if (normalizedMeta.contains(normalizedToken)) {
                score += 1.5D;
            }
        }
        return score;
    }

    private List<String> tokenize(String query) {
        String safeQuery = defaultText(query);
        Set<String> tokens = new LinkedHashSet<>();
        Matcher asciiMatcher = ASCII_TOKEN.matcher(safeQuery);
        while (asciiMatcher.find()) {
            addToken(tokens, asciiMatcher.group());
        }
        Matcher chineseMatcher = CHINESE_TOKEN.matcher(safeQuery);
        while (chineseMatcher.find()) {
            String block = chineseMatcher.group();
            addToken(tokens, block);
            for (int len = 2; len <= Math.min(4, block.length()); len++) {
                for (int i = 0; i + len <= block.length(); i++) {
                    addToken(tokens, block.substring(i, i + len));
                }
            }
        }
        if (tokens.isEmpty() && safeQuery.length() >= 2) {
            addToken(tokens, safeQuery);
        }
        return new ArrayList<>(tokens);
    }

    private void addToken(Set<String> tokens, String raw) {
        String token = normalize(raw);
        if (token.length() < 2 || STOP_WORDS.contains(token)) {
            return;
        }
        tokens.add(token);
    }

    private boolean containsAny(String text, Set<String> keywords) {
        String normalized = normalize(text);
        for (String keyword : keywords) {
            if (normalized.contains(normalize(keyword))) {
                return true;
            }
        }
        return false;
    }

    private double recencyBonus(LocalDateTime time, double max) {
        if (time == null) {
            return 0D;
        }
        long days = Math.max(Duration.between(time, LocalDateTime.now()).toDays(), 0);
        return Math.max(max - Math.min(days, (long) max), 0D);
    }

    private String formatTime(LocalDateTime time) {
        if (time == null) {
            return "";
        }
        return time.atZone(ZONE_ID).format(DATE_TIME_FORMATTER);
    }

    private String abbreviate(String text, int maxLength) {
        String value = defaultText(text);
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private String defaultText(String text) {
        return text == null ? "" : text.trim();
    }

    private String normalize(String text) {
        return defaultText(text).toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private Map<String, Object> toMap(AiRagCandidate candidate) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("sourceType", candidate.getSourceType().name());
        item.put("sourceId", candidate.getSourceId());
        item.put("title", candidate.getTitle());
        item.put("summary", candidate.getSummary());
        item.put("score", Math.round(candidate.getScore() * 100D) / 100D);
        item.put("route", candidate.getRoute());
        item.put("publishedAt", candidate.getPublishedAt());
        return item;
    }
}
