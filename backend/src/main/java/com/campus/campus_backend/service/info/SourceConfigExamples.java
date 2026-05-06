package com.campus.campus_backend.service.info;

public final class SourceConfigExamples {
    private SourceConfigExamples() {
    }

    public static final String WECHAT_RSS = """
            {
              "rssUrl": "https://rsshub.app/wechat/mp/<biz>",
              "maxItems": 20,
              "category": "活动",
              "tags": ["公众号", "校园"],
              "headers": {
                "User-Agent": "CampusCircle/1.0"
              }
            }
            """;

    public static final String ACADEMIC_HTML = """
            {
              "listUrl": "https://example.edu.cn/notice/list",
              "baseUrl": "https://example.edu.cn",
              "maxItems": 20,
              "category": "教务",
              "keyword": "通知 公告",
              "tags": ["教务"]
            }
            """;

    public static final String CANVAS_API = """
            {
              "baseUrl": "https://canvas.example.edu.cn",
              "token": "canvas-access-token",
              "announcementCourseIds": [101, 202],
              "includeGlobalAnnouncements": true,
              "includeTodo": true,
              "maxItems": 30,
              "category": "课程"
            }
            """;
}
