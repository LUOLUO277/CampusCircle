package com.campus.campus_backend.config;

import com.campus.campus_backend.domain.Category;
import com.campus.campus_backend.domain.Course;
import com.campus.campus_backend.domain.CourseCircleMember;
import com.campus.campus_backend.domain.CourseExperience;
import com.campus.campus_backend.domain.CourseQuestion;
import com.campus.campus_backend.domain.CourseQuestionReply;
import com.campus.campus_backend.domain.CourseReview;
import com.campus.campus_backend.domain.Post;
import com.campus.campus_backend.domain.SubscriptionSource;
import com.campus.campus_backend.domain.User;
import com.campus.campus_backend.repository.AggregatedNoticeRepository;
import com.campus.campus_backend.repository.CategoryRepository;
import com.campus.campus_backend.repository.CourseCircleMemberRepository;
import com.campus.campus_backend.repository.CourseExperienceRepository;
import com.campus.campus_backend.repository.CourseQuestionReplyRepository;
import com.campus.campus_backend.repository.CourseQuestionRepository;
import com.campus.campus_backend.repository.CourseRepository;
import com.campus.campus_backend.repository.CourseReviewRepository;
import com.campus.campus_backend.repository.NoticeSubscriptionRepository;
import com.campus.campus_backend.repository.PostRepository;
import com.campus.campus_backend.repository.SourceFetchLogRepository;
import com.campus.campus_backend.repository.SubscriptionSourceRepository;
import com.campus.campus_backend.repository.UserRepository;
import com.campus.campus_backend.service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Configuration
public class DataInitializer {
    @Bean
    public CommandLineRunner initUsers(UserRepository userRepository, UserService userService) {
        return args -> userRepository.findByUsername("admin").ifPresentOrElse(existing -> {
            if (!"ADMIN".equalsIgnoreCase(existing.getRole())) {
                existing.setRole("ADMIN");
                userRepository.save(existing);
            }
        }, () -> {
            User user = new User();
            user.setUsername("admin");
            user.setEmail("admin@example.com");
            user.setPasswordHash("admin123");
            user.setRole("ADMIN");
            userService.save(user);
        });
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
                    "把这周数据库课程的重点整理成了导图：事务隔离级别、索引失效场景、慢查询定位。需要的话我放评论区。",
                    "数据库课程重点整理", "课程,笔记", 186, 33, 14, 9, 86);
            seedPost(postRepository, u4, pickCategory(categories, 2),
                    "今天食堂新出的黑椒鸡排饭还不错，分量很足，建议 11:40 前去，晚了要排队。",
                    "食堂新菜测评", "美食,食堂", 158, 25, 11, 6, 73);
            seedPost(postRepository, u5, pickCategory(categories, 3),
                    "周六早上 7:00 东门集合跑 5km，配速友好，欢迎新手一起。跑完去吃早饭！",
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
                    Category category = new Category();
                    category.setName(name);
                    category.setSortOrder(order++);
                    category.setIsActive(true);
                    categoryRepository.save(category);
                }
            }
        };
    }

    @Bean
    public CommandLineRunner initSubscriptionSources(
            SubscriptionSourceRepository subscriptionSourceRepository,
            AggregatedNoticeRepository aggregatedNoticeRepository,
            NoticeSubscriptionRepository noticeSubscriptionRepository,
            SourceFetchLogRepository sourceFetchLogRepository) {
        return args -> {
            noticeSubscriptionRepository.deleteByMockSources();
            sourceFetchLogRepository.deleteByMockSources();
            aggregatedNoticeRepository.deleteByMockSources();
            aggregatedNoticeRepository.deleteByRawPayloadLike("\"mock\":true");
            subscriptionSourceRepository.findAll().stream()
                    .filter(source -> source.getFetchStrategy() != null
                            && source.getFetchStrategy().toUpperCase().startsWith("MOCK"))
                    .forEach(subscriptionSourceRepository::delete);

            seedSource(subscriptionSourceRepository, "人工补录", "MANUAL", "manual-notice",
                    null, "人工补录", "MANUAL");
        };
    }

    @Bean
    public CommandLineRunner initCourseSeedData(
            CourseRepository courseRepository,
            CourseCircleMemberRepository memberRepository,
            CourseQuestionRepository questionRepository,
            CourseQuestionReplyRepository replyRepository,
            CourseExperienceRepository experienceRepository,
            CourseReviewRepository reviewRepository,
            UserRepository userRepository,
            UserService userService) {
        return args -> {
            Map<String, Course> courses = new LinkedHashMap<>();
            courses.put("CS2101", seedCourse(courseRepository, "数据结构", "王立新", "CS2101", "计算机学院", "2026春季",
                    "围绕线性表、树、图与经典算法展开，适合作为算法基础与面试能力同步夯实的核心课程。",
                    "核心课,算法基础,期末笔试"));
            courses.put("CS2203", seedCourse(courseRepository, "数据库系统", "李晓晴", "CS2203", "软件学院", "2026春季",
                    "覆盖关系模型、SQL、事务并发控制与索引优化，强调数据库设计与工程实践。",
                    "SQL,实验课,项目驱动"));
            courses.put("SE2302", seedCourse(courseRepository, "软件工程", "陈思远", "SE2302", "软件学院", "2026春季",
                    "聚焦需求分析、架构设计、团队协作与测试交付，课程项目贯穿全学期。",
                    "团队项目,过程管理,汇报多"));
            courses.put("CS2404", seedCourse(courseRepository, "操作系统", "周明", "CS2404", "计算机学院", "2026春季",
                    "从进程线程、内存管理到文件系统和并发同步，内容抽象但收益很高。",
                    "理论硬核,并发,考前冲刺"));
            courses.put("CS2505", seedCourse(courseRepository, "计算机网络", "孙嘉", "CS2505", "计算机学院", "2026春季",
                    "讲解网络分层、路由传输和应用协议，搭配抓包实验帮助建立体系化认知。",
                    "协议分析,抓包实验,知识点密"));

            seedCourseCircleSamples(courses, memberRepository, questionRepository, replyRepository,
                    experienceRepository, reviewRepository, userRepository, userService);

            syncCourseCounts(courseRepository, memberRepository, questionRepository, experienceRepository, reviewRepository);
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

    private Course seedCourse(CourseRepository courseRepository, String name, String teacherName, String courseCode,
            String department, String semester, String description, String tags) {
        return courseRepository.findByCourseCodeIgnoreCase(courseCode).map(existing -> {
            existing.setName(name);
            existing.setTeacherName(teacherName);
            existing.setDepartment(department);
            existing.setSemester(semester);
            existing.setDescription(description);
            existing.setTags(tags);
            return courseRepository.save(existing);
        }).orElseGet(() -> {
            Course course = new Course();
            course.setName(name);
            course.setTeacherName(teacherName);
            course.setCourseCode(courseCode);
            course.setDepartment(department);
            course.setSemester(semester);
            course.setDescription(description);
            course.setTags(tags);
            return courseRepository.save(course);
        });
    }

    private void seedCourseCircleSamples(
            Map<String, Course> courses,
            CourseCircleMemberRepository memberRepository,
            CourseQuestionRepository questionRepository,
            CourseQuestionReplyRepository replyRepository,
            CourseExperienceRepository experienceRepository,
            CourseReviewRepository reviewRepository,
            UserRepository userRepository,
            UserService userService) {
        User admin = userRepository.findByUsername("admin").orElse(null);
        User u1 = seedUserIfMissing(userRepository, userService,
                "course_anna", "course_anna@example.com", "Campus@123", "安安", "同济大学", "偏爱整理课程知识点和复习清单");
        User u2 = seedUserIfMissing(userRepository, userService,
                "course_zhou", "course_zhou@example.com", "Campus@123", "周周", "同济大学", "经常在课程群里答疑和分享踩坑经验");
        User u3 = seedUserIfMissing(userRepository, userService,
                "course_lin", "course_lin@example.com", "Campus@123", "林同学", "同济大学", "做实验前会先看别人经验贴");
        User u4 = seedUserIfMissing(userRepository, userService,
                "course_he", "course_he@example.com", "Campus@123", "阿禾", "同济大学", "喜欢把实验环境和项目流程写明白");
        User u5 = seedUserIfMissing(userRepository, userService,
                "course_qi", "course_qi@example.com", "Campus@123", "琪琪", "同济大学", "选课前先看课程评价和学长学姐建议");
        User u6 = seedUserIfMissing(userRepository, userService,
                "course_peng", "course_peng@example.com", "Campus@123", "小彭", "同济大学", "考试周会回头补经验和资料索引");

        seedMembers(courses.get("CS2101"), List.of(u1, u2, u3, u4, u5, preferred(admin, u6)), memberRepository);
        seedMembers(courses.get("CS2203"), List.of(u1, u2, u4, u5, u6), memberRepository);
        seedMembers(courses.get("SE2302"), List.of(u2, u3, u4, u5), memberRepository);
        seedMembers(courses.get("CS2404"), List.of(u1, u3, u5, u6), memberRepository);
        seedMembers(courses.get("CS2505"), List.of(u2, u4, u6), memberRepository);

        CourseQuestion dsQ1 = seedQuestion(questionRepository, courses.get("CS2101"), u1,
                "期末复习图论部分怎么抓重点？",
                "老师最后两周讲得比较快，最短路、最小生成树和拓扑排序都提到了。想问往年复习是更看重手写过程还是复杂度分析？",
                "考试复习", false);
        seedReply(replyRepository, dsQ1, u2,
                "先把三类算法的适用场景分清，再准备一套手写模板。往年题通常会让你解释为什么选这个算法。");
        seedReply(replyRepository, dsQ1, u6,
                "如果时间紧，优先把图的遍历、最短路和并查集这几个高频点过一遍。");
        questionRepository.save(dsQ1);

        CourseQuestion dsQ2 = seedQuestion(questionRepository, courses.get("CS2101"), u3,
                "链表实验提交后一直卡在内存泄漏检查",
                "本地样例和 OJ 结果都对，但是提交后显示 leak。有没有同学踩过析构或者 dummy node 没释放的问题？",
                "实验环境", true);
        seedReply(replyRepository, dsQ2, u4,
                "大概率是尾节点删除后没有把 next 断开，或者你 new 了辅助节点但提前 return 了。");
        questionRepository.save(dsQ2);

        CourseQuestion dbQ1 = seedQuestion(questionRepository, courses.get("CS2203"), u4,
                "数据库实验三需要自己建索引吗？",
                "实验说明里只说分析执行计划，没有写一定要建索引。报告部分是不是要自己对比建索引前后的查询时间？",
                "作业问题", false);
        seedReply(replyRepository, dbQ1, u1,
                "我们组是自己补了索引对比，报告里把 explain 截图放进去，老师给分会更稳。");
        seedReply(replyRepository, dbQ1, u5,
                "建议至少试主键索引和组合索引两种，不然优化部分会比较空。");
        questionRepository.save(dbQ1);

        CourseQuestion seQ1 = seedQuestion(questionRepository, courses.get("SE2302"), u5,
                "软件工程大作业组队是不是要跨方向搭配？",
                "听说老师比较看重前后端和测试角色分工，都是纯开发同学会不会在答辩时吃亏？",
                "项目组队", false);
        seedReply(replyRepository, seQ1, u3,
                "最好还是把文档、测试、开发都有人负责，答辩时老师会直接追问谁做了需求和测试。");
        questionRepository.save(seQ1);

        CourseQuestion osQ1 = seedQuestion(questionRepository, courses.get("CS2404"), u6,
                "操作系统线程同步题有什么刷题顺序推荐？",
                "信号量、管程和经典生产者消费者题型总是混。想知道大家一般先刷哪一类更容易建立套路。",
                "考试复习", false);
        seedReply(replyRepository, osQ1, u1,
                "先刷互斥和同步分开的基础题，再看生产者消费者、读者写者、哲学家三类经典模型。");
        questionRepository.save(osQ1);

        CourseQuestion netQ1 = seedQuestion(questionRepository, courses.get("CS2505"), u2,
                "计网抓包实验的 DNS 部分要分析到什么程度？",
                "报告模板比较简略，我现在只写了请求响应流程和字段截图，不确定够不够。",
                "作业问题", true);
        seedReply(replyRepository, netQ1, u4,
                "把递归查询和迭代查询的区别补上，再解释几个常见标志位基本就够用了。");
        questionRepository.save(netQ1);

        seedExperience(experienceRepository, courses.get("CS2101"), u2, "2025秋季", "王立新", 4, 3, 5, 4, 5,
                "平时一定要自己手写一遍树和图的常用模板，尤其是 BFS、DFS、并查集和堆。老师课堂节奏快，但逻辑很清楚，跟住每周例题基本不会掉队。",
                "不要把重点全压到考试周，数据结构如果前面数组链表没打牢，后面树图会很难补。实验里指针细节特别多，建议边写边画图。",
                "适合想补算法基础、准备竞赛或面试的同学。");
        seedExperience(experienceRepository, courses.get("CS2203"), u1, "2025秋季", "李晓晴", 3, 4, 5, 3, 5,
                "SQL 部分一定要边学边敲，实验报告如果只贴结果没有解释会吃亏。老师对范式、事务隔离和索引设计讲得很实用，和后端开发联系很强。",
                "很多同学忽视 explain 和慢查询分析，结果项目部分只停留在会写 CRUD。建议把建表、索引、事务三块都自己总结成模板。",
                "适合后端方向、数据方向，以及想把工程能力补扎实的同学。");
        seedExperience(experienceRepository, courses.get("SE2302"), u4, "2025秋季", "陈思远", 3, 5, 4, 4, 4,
                "这门课的关键不是单点技术，而是流程。需求、原型、接口、测试、汇报都要有人兜住。组内最好尽早建立文档规范和站会节奏。",
                "别等到中期检查前才开始整合代码。项目越到后面越容易被协作问题拖慢，版本管理和分工记录一定要留痕。",
                "适合愿意做团队协作、以后会做项目管理或全栈交付的同学。");
        seedExperience(experienceRepository, courses.get("CS2404"), u3, "2025秋季", "周明", 5, 4, 5, 5, 4,
                "操作系统要结合图和过程理解，不要死背定义。进程调度、分页、死锁和同步机制是主线，建议自己画状态转换图和时序图。",
                "考前只刷题不补概念会很痛苦，尤其是同步题。老师题目喜欢换表述，但本质模型固定，平时最好分类整理。",
                "适合愿意啃理论、后续还会学系统和底层方向课程的同学。");
        seedExperience(experienceRepository, courses.get("CS2505"), u6, "2025秋季", "孙嘉", 3, 3, 4, 3, 4,
                "计网内容碎但主线明确，先把五层模型和每层职责搞清楚，再看典型协议。抓包实验非常加分，能把抽象概念落地。",
                "不要一上来背协议字段，先理解为什么需要这个协议。实验报告里如果只贴截图而没有分析，很难体现你真的看懂了包。",
                "适合想补基础、准备面试八股或者网络方向入门的同学。");

        seedReview(reviewRepository, courses.get("CS2101"), u1, 4, 3, 5, 5, 4, 5,
                "讲得扎实，题目不水，适合认真打基础。", "算法基础,复习收益高");
        seedReview(reviewRepository, courses.get("CS2101"), u3, 4, 4, 5, 4, 4, 5,
                "平时实验稍微花时间，但学完以后对刷题帮助明显。", "实验细节多,适合面试");
        seedReview(reviewRepository, courses.get("CS2203"), u2, 3, 4, 5, 5, 3, 5,
                "数据库设计和 SQL 优化部分讲得很清楚，项目也比较贴近实际。", "SQL实用,项目驱动");
        seedReview(reviewRepository, courses.get("CS2203"), u5, 3, 4, 4, 4, 3, 4,
                "实验周会忙一点，但内容很值。", "实验偏多,后端相关");
        seedReview(reviewRepository, courses.get("SE2302"), u4, 3, 5, 4, 4, 4, 4,
                "适合愿意认真做团队项目的人，纯摸鱼会很难受。", "协作要求高,文档多");
        seedReview(reviewRepository, courses.get("CS2404"), u6, 5, 4, 5, 4, 5, 4,
                "难度高，但理解透之后后续系统课都会轻松很多。", "理论硬核,期末压力大");
        seedReview(reviewRepository, courses.get("CS2404"), u1, 4, 4, 5, 4, 4, 4,
                "老师推导过程很完整，适合耐心听课。", "系统基础,同步难点多");
        seedReview(reviewRepository, courses.get("CS2505"), u2, 3, 3, 4, 4, 3, 4,
                "抓包实验很有帮助，整体节奏中规中矩。", "实验友好,知识点碎");
    }

    private User preferred(User preferred, User fallback) {
        return preferred != null ? preferred : fallback;
    }

    private void seedMembers(Course course, List<User> users, CourseCircleMemberRepository memberRepository) {
        if (course == null) {
            return;
        }
        for (User user : users.stream().filter(Objects::nonNull).toList()) {
            if (memberRepository.existsByCourseIdAndUserId(course.getId(), user.getId())) {
                continue;
            }
            CourseCircleMember member = new CourseCircleMember();
            member.setCourse(course);
            member.setUser(user);
            memberRepository.save(member);
        }
    }

    private CourseQuestion seedQuestion(CourseQuestionRepository repository, Course course, User user,
            String title, String content, String questionType, boolean resolved) {
        if (repository.existsByCourseIdAndTitle(course.getId(), title)) {
            return repository.findByCourseIdOrderByCreatedAtDesc(course.getId()).stream()
                    .filter(item -> title.equals(item.getTitle()))
                    .findFirst()
                    .orElseThrow();
        }
        CourseQuestion question = new CourseQuestion();
        question.setCourse(course);
        question.setUser(user);
        question.setTitle(title);
        question.setContent(content);
        question.setQuestionType(questionType);
        question.setResolved(resolved);
        question.setReplyCount(0);
        return repository.save(question);
    }

    private void seedReply(CourseQuestionReplyRepository repository, CourseQuestion question, User user, String content) {
        if (repository.existsByQuestionIdAndUserIdAndContent(question.getId(), user.getId(), content)) {
            return;
        }
        CourseQuestionReply reply = new CourseQuestionReply();
        reply.setQuestion(question);
        reply.setUser(user);
        reply.setContent(content);
        repository.save(reply);
        question.setReplyCount(question.getReplyCount() + 1);
    }

    private void seedExperience(CourseExperienceRepository repository, Course course, User user,
            String semester, String teacherName, int difficulty, int workload, int gain, int examPressure,
            int recommend, String studyAdvice, String pitfallAdvice, String suitableFor) {
        if (repository.existsByCourseIdAndUserIdAndSemester(course.getId(), user.getId(), semester)) {
            return;
        }
        CourseExperience experience = new CourseExperience();
        experience.setCourse(course);
        experience.setUser(user);
        experience.setSemester(semester);
        experience.setTeacherName(teacherName);
        experience.setDifficultyRating(difficulty);
        experience.setWorkloadRating(workload);
        experience.setGainRating(gain);
        experience.setExamPressureRating(examPressure);
        experience.setRecommendRating(recommend);
        experience.setStudyAdvice(studyAdvice);
        experience.setPitfallAdvice(pitfallAdvice);
        experience.setSuitableFor(suitableFor);
        repository.save(experience);
    }

    private void seedReview(CourseReviewRepository repository, Course course, User user,
            int difficulty, int workload, int gain, int clarity, int examPressure, int recommend,
            String content, String tags) {
        if (repository.findByCourseIdAndUserId(course.getId(), user.getId()).isPresent()) {
            return;
        }
        CourseReview review = new CourseReview();
        review.setCourse(course);
        review.setUser(user);
        review.setDifficultyRating(difficulty);
        review.setWorkloadRating(workload);
        review.setGainRating(gain);
        review.setClarityRating(clarity);
        review.setExamPressureRating(examPressure);
        review.setRecommendRating(recommend);
        review.setContent(content);
        review.setTags(tags);
        repository.save(review);
    }

    private void syncCourseCounts(CourseRepository courseRepository,
            CourseCircleMemberRepository memberRepository,
            CourseQuestionRepository questionRepository,
            CourseExperienceRepository experienceRepository,
            CourseReviewRepository reviewRepository) {
        for (Course course : courseRepository.findAll()) {
            course.setMemberCount((int) memberRepository.countByCourseId(course.getId()));
            course.setQuestionCount((int) questionRepository.countByCourseId(course.getId()));
            course.setExperienceCount((int) experienceRepository.countByCourseId(course.getId()));
            course.setReviewCount((int) reviewRepository.countByCourseId(course.getId()));
            courseRepository.save(course);
        }
    }
}
