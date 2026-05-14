package com.campus.campus_backend.config;

import com.campus.campus_backend.domain.User;
import com.campus.campus_backend.domain.Category;
import com.campus.campus_backend.domain.SubscriptionSource;
import com.campus.campus_backend.repository.AggregatedNoticeRepository;
import com.campus.campus_backend.repository.NoticeSubscriptionRepository;
import com.campus.campus_backend.repository.SourceFetchLogRepository;
import com.campus.campus_backend.repository.UserRepository;
import com.campus.campus_backend.repository.CategoryRepository;
import com.campus.campus_backend.repository.SubscriptionSourceRepository;
import com.campus.campus_backend.service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;
import java.util.List;

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
}
