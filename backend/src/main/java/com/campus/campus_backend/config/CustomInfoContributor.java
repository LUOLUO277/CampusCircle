package com.campus.campus_backend.config;

import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Component
public class CustomInfoContributor implements InfoContributor {

    @Override
    public void contribute(Info.Builder builder) {
        Map<String, Object> details = new HashMap<>();

        details.put("name", "Campus Info Center");
        details.put("version", "1.0.0");
        details.put("description", "校园信息订阅中心后端 API 服务");
        details.put("startup-time", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        details.put("java-version", System.getProperty("java.version"));
        details.put("spring-boot-version", "3.3.5");

        Map<String, Object> modules = new HashMap<>();
        modules.put("authentication", "JWT 认证系统");
        modules.put("content", "校园内容与帖子管理");
        modules.put("info-center", "信息订阅中心");
        modules.put("canvas", "Canvas 绑定与通知拉取");
        modules.put("monitoring", "系统监控");
        details.put("modules", modules);

        Map<String, Object> retiredModules = new HashMap<>();
        retiredModules.put("errands", "功能下线，代码保留待后续清理");
        retiredModules.put("messaging", "功能下线，代码保留待后续清理");
        details.put("retired-modules", retiredModules);

        Map<String, Object> deployment = new HashMap<>();
        deployment.put("server", "Windows Server");
        deployment.put("web-server", "IIS + ARR");
        deployment.put("database", "MySQL 8.0");
        deployment.put("cache", "Redis 6.0");
        details.put("deployment", deployment);

        builder.withDetails(details);
    }
}
