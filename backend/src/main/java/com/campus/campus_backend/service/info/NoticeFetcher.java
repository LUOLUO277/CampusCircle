package com.campus.campus_backend.service.info;

import com.campus.campus_backend.domain.SubscriptionSource;

import java.util.List;

public interface NoticeFetcher {
    boolean supports(SubscriptionSource source);
    List<RawNoticeItem> fetch(SubscriptionSource source);
}
