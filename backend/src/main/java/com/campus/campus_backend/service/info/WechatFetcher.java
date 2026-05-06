package com.campus.campus_backend.service.info;

import com.campus.campus_backend.domain.SubscriptionSource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class WechatFetcher implements NoticeFetcher {
    private final RemoteNoticeSupport remoteNoticeSupport;

    public WechatFetcher(RemoteNoticeSupport remoteNoticeSupport) {
        this.remoteNoticeSupport = remoteNoticeSupport;
    }

    @Override
    public boolean supports(SubscriptionSource source) {
        return "WECHAT".equalsIgnoreCase(source.getType())
                && List.of("WECHAT_RSS", "WECHAT_HTML").contains(String.valueOf(source.getFetchStrategy()).toUpperCase());
    }

    @Override
    public List<RawNoticeItem> fetch(SubscriptionSource source) {
        Map<String, Object> config = remoteNoticeSupport.readConfig(source);
        String strategy = String.valueOf(source.getFetchStrategy()).toUpperCase();
        if ("WECHAT_RSS".equals(strategy)) {
            String rssUrl = remoteNoticeSupport.firstNonBlank(
                    remoteNoticeSupport.stringValue(config.get("rssUrl")),
                    source.getSourceUrl());
            String xml = remoteNoticeSupport.fetchText(rssUrl, config);
            return remoteNoticeSupport.parseRss(xml, source, config);
        }
        String htmlUrl = remoteNoticeSupport.firstNonBlank(
                remoteNoticeSupport.stringValue(config.get("listUrl")),
                source.getSourceUrl());
        String html = remoteNoticeSupport.fetchText(htmlUrl, config);
        return remoteNoticeSupport.parseHtmlAnchors(html, source, config);
    }
}
