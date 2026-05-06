package com.campus.campus_backend.service.info;

import com.campus.campus_backend.domain.SubscriptionSource;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
public class MockNoticeFetcher implements NoticeFetcher {
    @Override
    public boolean supports(SubscriptionSource source) {
        return source.getFetchStrategy() != null && source.getFetchStrategy().startsWith("MOCK");
    }

    @Override
    public List<RawNoticeItem> fetch(SubscriptionSource source) {
        LocalDateTime now = LocalDateTime.now();
        return List.of(
                RawNoticeItem.builder()
                        .externalId(source.getSourceKey() + "-demo-1")
                        .title(source.getName() + " 本周通知汇总")
                        .content("面向全体同学开放报名，截止时间 2026-05-12 18:00，地点 四平路校区综合楼 101，详见原文。")
                        .originalUrl(source.getSourceUrl())
                        .category("通知")
                        .publishTime(now.minusHours(2))
                        .rawPayload(Map.of("mock", true, "sourceKey", source.getSourceKey()))
                        .tags(List.of("报名", "校内"))
                        .build(),
                RawNoticeItem.builder()
                        .externalId(source.getSourceKey() + "-demo-2")
                        .title(source.getName() + " 截止提醒")
                        .content("适用对象：本科生。请于 2026-05-15 23:59 前完成提交，入口 https://canvas.tongji.edu.cn/calendar")
                        .originalUrl(source.getSourceUrl())
                        .category("截止提醒")
                        .publishTime(now.minusDays(1))
                        .rawPayload(Map.of("mock", true, "batch", 2))
                        .tags(List.of("截止", "提醒"))
                        .build());
    }
}
