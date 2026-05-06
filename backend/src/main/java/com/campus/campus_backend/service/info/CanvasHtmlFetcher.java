package com.campus.campus_backend.service.info;

import com.campus.campus_backend.domain.SubscriptionSource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class CanvasHtmlFetcher implements NoticeFetcher {
    private final RemoteNoticeSupport remoteNoticeSupport;

    public CanvasHtmlFetcher(RemoteNoticeSupport remoteNoticeSupport) {
        this.remoteNoticeSupport = remoteNoticeSupport;
    }

    @Override
    public boolean supports(SubscriptionSource source) {
        return "CANVAS".equalsIgnoreCase(source.getType()) && "CANVAS_HTML".equalsIgnoreCase(source.getFetchStrategy());
    }

    @Override
    public List<RawNoticeItem> fetch(SubscriptionSource source) {
        Map<String, Object> config = remoteNoticeSupport.readConfig(source);
        String html = remoteNoticeSupport.fetchText(
                remoteNoticeSupport.firstNonBlank(remoteNoticeSupport.stringValue(config.get("listUrl")), source.getSourceUrl()),
                config);
        return remoteNoticeSupport.parseHtmlAnchors(html, source, config);
    }
}
