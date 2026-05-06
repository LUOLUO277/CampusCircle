package com.campus.campus_backend.config;

import com.campus.campus_backend.domain.User;
import com.campus.campus_backend.domain.Category;
import com.campus.campus_backend.domain.SubscriptionSource;
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
    public CommandLineRunner initSubscriptionSources(SubscriptionSourceRepository subscriptionSourceRepository) {
        return args -> {
            seedSource(subscriptionSourceRepository, "同济大学", "WECHAT", "tongji-university-wechat",
                    "https://mp.weixin.qq.com", "同济大学 公众号", "MOCK_WECHAT");
            seedSource(subscriptionSourceRepository, "同济体育", "WECHAT", "tongji-sports-wechat",
                    "https://mp.weixin.qq.com", "同济体育 公众号 体育", "MOCK_WECHAT");
            seedSource(subscriptionSourceRepository, "同济教务公告", "ACADEMIC", "tongji-academic-announcement",
                    "https://1.tongji.edu.cn/myAnnouncement", "教务 公告", "MOCK_ACADEMIC");
            seedSource(subscriptionSourceRepository, "Canvas 日历", "CANVAS", "tongji-canvas-calendar",
                    "https://canvas.tongji.edu.cn/calendar", "canvas 日历 作业", "MOCK_CANVAS");
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
