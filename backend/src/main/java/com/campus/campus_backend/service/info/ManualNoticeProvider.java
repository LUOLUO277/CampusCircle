package com.campus.campus_backend.service.info;

import com.campus.campus_backend.domain.SubscriptionSource;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ManualNoticeProvider implements NoticeFetcher {
    @Override
    public boolean supports(SubscriptionSource source) {
        return "MANUAL".equalsIgnoreCase(source.getType());
    }

    @Override
    public List<RawNoticeItem> fetch(SubscriptionSource source) {
        return List.of();
    }
}
