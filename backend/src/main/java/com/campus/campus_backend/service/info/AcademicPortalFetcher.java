package com.campus.campus_backend.service.info;

import com.campus.campus_backend.domain.SubscriptionSource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class AcademicPortalFetcher implements NoticeFetcher {
    private final RemoteNoticeSupport remoteNoticeSupport;

    public AcademicPortalFetcher(RemoteNoticeSupport remoteNoticeSupport) {
        this.remoteNoticeSupport = remoteNoticeSupport;
    }

    @Override
    public boolean supports(SubscriptionSource source) {
        return "ACADEMIC".equalsIgnoreCase(source.getType())
                && List.of("ACADEMIC_HTML", "ACADEMIC_RSS").contains(String.valueOf(source.getFetchStrategy()).toUpperCase());
    }

    @Override
    public List<RawNoticeItem> fetch(SubscriptionSource source) {
        Map<String, Object> config = remoteNoticeSupport.readConfig(source);
        String strategy = String.valueOf(source.getFetchStrategy()).toUpperCase();
        if ("ACADEMIC_RSS".equals(strategy)) {
            String xml = remoteNoticeSupport.fetchText(
                    remoteNoticeSupport.firstNonBlank(remoteNoticeSupport.stringValue(config.get("rssUrl")), source.getSourceUrl()),
                    config);
            return remoteNoticeSupport.parseRss(xml, source, config);
        }
        String html = remoteNoticeSupport.fetchText(
                remoteNoticeSupport.firstNonBlank(remoteNoticeSupport.stringValue(config.get("listUrl")), source.getSourceUrl()),
                config);
        return remoteNoticeSupport.parseHtmlAnchors(html, source, config);
    }
}
