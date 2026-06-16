package com.campus.campus_backend.config;

import com.campus.campus_backend.domain.User;
import com.campus.campus_backend.domain.Category;
import com.campus.campus_backend.domain.Course;
import com.campus.campus_backend.domain.Post;
import com.campus.campus_backend.domain.SubscriptionSource;
import com.campus.campus_backend.repository.AggregatedNoticeRepository;
import com.campus.campus_backend.repository.CourseRepository;
import com.campus.campus_backend.repository.NoticeSubscriptionRepository;
import com.campus.campus_backend.repository.SourceFetchLogRepository;
import com.campus.campus_backend.repository.UserRepository;
import com.campus.campus_backend.repository.CategoryRepository;
import com.campus.campus_backend.repository.PostRepository;
import com.campus.campus_backend.repository.SubscriptionSourceRepository;
import com.campus.campus_backend.service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;
import java.util.List;
import java.util.ArrayList;

@Configuration
public class DataInitializer {
    @Bean
    public CommandLineRunner initUsers(UserRepository userRepository, UserService userService) {
        return args -> {
            userRepository.findByUsername("admin").ifPresentOrElse(existing -> {
                if (!"ADMIN".equalsIgnoreCase(existing.getRole())) {
                    existing.setRole("ADMIN");
                    userRepository.save(existing);
                }
            }, () -> {
                User u = new User();
                u.setUsername("admin");
                u.setEmail("admin@example.com");
                u.setPasswordHash("admin123");
                u.setRole("ADMIN");
                userService.save(u);
            });
        };
    }

    @Bean
    public CommandLineRunner initCommunitySeedData(
            UserRepository userRepository,
            UserService userService,
            CategoryRepository categoryRepository,
            PostRepository postRepository) {
        return args -> {
            if (postRepository.count() > 0) {
                return;
            }

            User u1 = seedUserIfMissing(userRepository, userService,
                    "xiaoyu01", "xiaoyu01@example.com", "Campus@123", "小宇", "同济大学", "热衷校园活动，周末常拍照记录校园");
            User u2 = seedUserIfMissing(userRepository, userService,
                    "linlin02", "linlin02@example.com", "Campus@123", "林林", "同济大学", "二手交换爱好者，主打实用和靠谱");
            User u3 = seedUserIfMissing(userRepository, userService,
                    "momo03", "momo03@example.com", "Campus@123", "Momo", "同济大学", "经常分享课程笔记和学习心得");
            User u4 = seedUserIfMissing(userRepository, userService,
                    "qianqian04", "qianqian04@example.com", "Campus@123", "浅浅", "同济大学", "美食雷达，食堂和周边店都在测评");
            User u5 = seedUserIfMissing(userRepository, userService,
                    "ajie05", "ajie05@example.com", "Campus@123", "阿杰", "同济大学", "跑步打卡中，欢迎约晨跑");
            User u6 = seedUserIfMissing(userRepository, userService,
                    "ting06", "ting06@example.com", "Campus@123", "小婷", "同济大学", "关注社团资讯和志愿活动");

            List<Category> categories = new ArrayList<>(categoryRepository.findAll());
            if (categories.isEmpty()) {
                return;
            }
            categories.sort((a, b) -> Integer.compare(a.getSortOrder(), b.getSortOrder()));

            seedPost(postRepository, u1, pickCategory(categories, 0),
                    "今天图书馆二楼西侧位置很充足，空调温度也很舒服，复习效率直接拉满。有人晚上一起自习吗？",
                    "图书馆晚间自习约伴", "学习,自习", 132, 18, 7, 5, 61);
            seedPost(postRepository, u2, pickCategory(categories, 1),
                    "出一台九成新小米台灯，宿舍自提，今晚或明天中午都可以。需要的同学私我～",
                    "二手台灯转让", "二手,宿舍", 97, 12, 4, 3, 46);
            seedPost(postRepository, u3, pickCategory(categories, 2),
                    "把这周数据库课程的重点整理成了导图：事务隔离级别、索引失效场景、慢查询定位。需要的我放评论区。",
                    "数据库课程重点整理", "课程,笔记", 186, 33, 14, 9, 86);
            seedPost(postRepository, u4, pickCategory(categories, 2),
                    "今天食堂新出的黑椒鸡排饭还不错，分量很足，建议 11:40 前去，晚了要排队。",
                    "食堂新菜测评", "美食,食堂", 158, 25, 11, 6, 73);
            seedPost(postRepository, u5, pickCategory(categories, 3),
                    "周六早上 7:00 东门集合跑 5km，配速友好，欢迎新手一起。跑完去吃早餐！",
                    "周末晨跑招募", "运动,跑步", 121, 21, 8, 4, 58);
            seedPost(postRepository, u6, pickCategory(categories, 4),
                    "这学期你最希望校圈新增哪个功能？A.活动报名 B.课程互助 C.二手置顶 D.夜聊广场，欢迎投票讨论。",
                    "校圈功能投票", "投票,建议", 144, 29, 16, 5, 77);
            seedPost(postRepository, u1, pickCategory(categories, 2),
                    "操场晚霞太好看了，随手拍了几张。考试周也别忘了抬头看看天空，心情会好很多。",
                    "晚霞打卡", "校园,日常", 111, 19, 6, 2, 52);
            seedPost(postRepository, u3, pickCategory(categories, 1),
                    "求助：有没有同学了解本周选修课调课信息？教务系统里两个时间版本不一致。",
                    "选修课调课求助", "求助,教务", 89, 10, 9, 1, 41);
            seedPost(postRepository, u2, pickCategory(categories, 0),
                    "宿舍楼下快递柜今天有点堵，建议大家避开 18:00-19:00 高峰时段取件。",
                    "快递柜高峰提醒", "生活,提醒", 76, 8, 5, 1, 35);
            seedPost(postRepository, u5, pickCategory(categories, 2),
                    "刚在实验楼旁发现一个安静角落，插座多、人少，赶 project 的同学可以试试。",
                    "赶作业安静点位分享", "学习,地点", 104, 17, 7, 3, 49);
        };
    }

    @Bean
    public CommandLineRunner initCategories(CategoryRepository categoryRepository) {
        return args -> {
            List<String> preset = List.of("闲置", "求助", "日常生活", "投票", "吐槽");
            boolean needSeed = categoryRepository.count() == 0;
            if (!needSeed) {
                long existing = categoryRepository.findAll().stream()
                        .map(Category::getName)
                        .filter(preset::contains)
                        .count();
                needSeed = existing < preset.size();
            }
            if (needSeed) {
                int order = 1;
                for (String name : preset) {
                    boolean exists = categoryRepository.findAll().stream().anyMatch(c -> name.equals(c.getName()));
                    if (exists) {
                        continue;
                    }
                    Category c = new Category();
                    c.setName(name);
                    c.setSortOrder(order++);
                    c.setIsActive(true);
                    categoryRepository.save(c);
                }
            }
        };
    }

    @Bean
    public CommandLineRunner initSubscriptionSources(SubscriptionSourceRepository subscriptionSourceRepository,
            AggregatedNoticeRepository aggregatedNoticeRepository,
            NoticeSubscriptionRepository noticeSubscriptionRepository,
            SourceFetchLogRepository sourceFetchLogRepository) {
        return args -> {
            // 清理所有 mock 源/日志/通知，避免模拟通知影响判断
            // 1) 先删订阅/日志/通知（避免外键约束）
            noticeSubscriptionRepository.deleteByMockSources();
            sourceFetchLogRepository.deleteByMockSources();
            aggregatedNoticeRepository.deleteByMockSources();
            // 2) 再按 rawPayload 标记兜底清理（历史遗留/源已被改名等）
            aggregatedNoticeRepository.deleteByRawPayloadLike("\"mock\":true");
            // 3) 最后删除所有 MOCK* 抓取策略的源
            subscriptionSourceRepository.findAll().stream()
                    .filter(source -> source.getFetchStrategy() != null
                            && source.getFetchStrategy().toUpperCase().startsWith("MOCK"))
                    .forEach(subscriptionSourceRepository::delete);

            seedSource(subscriptionSourceRepository, "人工补录", "MANUAL", "manual-notice",
                    null, "人工 补录", "MANUAL");
        };
    }

    @Bean
    public CommandLineRunner initCourseSeedData(CourseRepository courseRepository) {
        return args -> {
            if (courseRepository.count() > 0) {
                return;
            }

            seedCourse(courseRepository, "数据结构", "王立新", "CS2101", "计算机学院", "2026春季",
                    "围绕线性表、树、图与经典算法展开，适合算法基础与面试能力同步夯实。",
                    "核心课,算法基础,期末笔试", 128, 36, 18, 24);
            seedCourse(courseRepository, "数据库系统", "李晓晨", "CS2203", "软件学院", "2026春季",
                    "覆盖关系模型、SQL、事务并发控制与索引优化，强调数据库设计与工程实践。",
                    "SQL,实验课,项目驱动", 152, 44, 21, 29);
            seedCourse(courseRepository, "软件工程", "陈思远", "SE2302", "软件学院", "2026春季",
                    "聚焦需求分析、架构设计、团队协作与测试交付，课程项目贯穿全学期。",
                    "团队项目,过程管理,汇报多", 96, 19, 26, 17);
            seedCourse(courseRepository, "操作系统", "周明", "CS2404", "计算机学院", "2026春季",
                    "从进程线程、内存管理到文件系统和并发同步，内容抽象但收益很高。",
                    "理论硬核,并发,考前冲刺", 141, 39, 16, 25);
            seedCourse(courseRepository, "数据挖掘", "赵宁", "AI3102", "人工智能学院", "2026春季",
                    "结合分类、聚类与特征工程案例，适合希望做竞赛和科研入门的同学。",
                    "机器学习,实验报告,案例多", 87, 14, 13, 12);
            seedCourse(courseRepository, "计算机网络", "孙嘉", "CS2505", "计算机学院", "2026春季",
                    "讲解网络分层、路由传输和应用协议，搭配抓包实验帮助建立体系化认知。",
                    "协议分析,抓包实验,知识点密", 118, 28, 15, 20);
        };
    }

    private void seedSource(SubscriptionSourceRepository repository, String name, String type, String sourceKey,
            String sourceUrl, String searchKeywords, String fetchStrategy) {
        if (repository.findBySourceKey(sourceKey).isPresent()) {
            return;
        }
        SubscriptionSource source = new SubscriptionSource();
        source.setName(name);
        source.setType(type);
        source.setSourceKey(sourceKey);
        source.setSourceUrl(sourceUrl);
        source.setSearchKeywords(searchKeywords);
        source.setFetchStrategy(fetchStrategy);
        source.setFetchConfigJson("{}");
        source.setStatus("ACTIVE");
        source.setLastFetchStatus("PENDING");
        repository.save(source);
    }

    private User seedUserIfMissing(UserRepository userRepository, UserService userService,
            String username, String email, String password,
            String nickname, String school, String bio) {
        return userRepository.findByUsername(username).orElseGet(() -> {
            User user = new User();
            user.setUsername(username);
            user.setEmail(email);
            user.setPasswordHash(password);
            user.setRole("USER");
            user.setNickname(nickname);
            user.setSchool(school);
            user.setBio(bio);
            user.setPoints(120);
            return userService.save(user);
        });
    }

    private Category pickCategory(List<Category> categories, int index) {
        if (categories.isEmpty()) {
            return null;
        }
        return categories.get(index % categories.size());
    }

    private void seedPost(PostRepository postRepository, User user, Category category,
            String content, String summary, String tags,
            int viewCount, int likeCount, int commentCount, int collectCount, int hotScore) {
        if (user == null || category == null) {
            return;
        }
        Post post = new Post();
        post.setUser(user);
        post.setCategory(category);
        post.setIsAnonymous(false);
        post.setContent(content);
        post.setSummary(summary);
        post.setTags(tags);
        post.setImages("[]");
        post.setViewCount(viewCount);
        post.setLikeCount(likeCount);
        post.setCommentCount(commentCount);
        post.setCollectCount(collectCount);
        post.setHotScore(hotScore);
        post.setIsSticky(false);
        post.setStatus((short) 0);
        postRepository.save(post);
    }

    private void seedCourse(CourseRepository courseRepository, String name, String teacherName, String courseCode,
            String department, String semester, String description, String tags,
            int memberCount, int questionCount, int experienceCount, int reviewCount) {
        Course course = new Course();
        course.setName(name);
        course.setTeacherName(teacherName);
        course.setCourseCode(courseCode);
        course.setDepartment(department);
        course.setSemester(semester);
        course.setDescription(description);
        course.setTags(tags);
        course.setMemberCount(memberCount);
        course.setQuestionCount(questionCount);
        course.setExperienceCount(experienceCount);
        course.setReviewCount(reviewCount);
        courseRepository.save(course);
    }
}
