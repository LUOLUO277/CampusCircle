package com.campus.campus_backend.service.ai;

import com.campus.campus_backend.domain.AggregatedNotice;
import com.campus.campus_backend.domain.Post;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AiPromptBuilder {
    public String buildNoticeQueryPrompt(String question, List<AggregatedNotice> candidates) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
                你是“校圈”的校园通知助手。你只能根据给定通知列表回答，不能编造不存在的通知。
                如果没有相关内容，answer 必须是“未找到相关通知”，matchedNotices 必须是空数组。
                请返回 JSON，格式如下：
                {
                  "answer":"...",
                  "matchedNotices":[
                    {
                      "id":1,
                      "title":"...",
                      "publishTime":"yyyy-MM-dd HH:mm:ss",
                      "category":"课程作业|考试相关|竞赛活动|生活服务|其他",
                      "summary":"...",
                      "reason":"..."
                    }
                  ]
                }
                用户问题：
                """).append(question).append("\n\n通知候选列表：\n");
        for (AggregatedNotice n : candidates) {
            sb.append("- id=").append(n.getId())
                    .append("; title=").append(n.getTitle())
                    .append("; publishTime=").append(formatDateTime(n.getPublishTime()))
                    .append("; category=").append(defaultText(n.getCategory(), "其他"))
                    .append("; summary=").append(defaultText(n.getSummary(), ""))
                    .append("; content=").append(defaultText(n.getContentSnapshot(), ""))
                    .append("\n");
        }
        return sb.toString();
    }

    public String buildNoticeCategoryPrompt(AggregatedNotice notice) {
        return """
                你是校园通知分类助手。请将通知归入以下固定类别之一：
                课程作业、考试相关、竞赛活动、生活服务、其他。
                只能输出 JSON，不要输出其他文本。格式：
                {"category":"课程作业|考试相关|竞赛活动|生活服务|其他","reason":"..."}
                无法判断时输出“其他”。
                通知内容如下：
                """
                + "\ntitle: " + notice.getTitle()
                + "\nsummary: " + defaultText(notice.getSummary(), "")
                + "\ncontent: " + defaultText(notice.getContentSnapshot(), "");
    }

    public String buildPostSearchPrompt(String question, List<Post> candidates) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
                你是“校圈”的帖子智能搜索助手。你只能根据给定帖子候选列表回答，不能编造不存在的帖子。
                如果没有相关帖子，answer 必须是“未找到相关帖子”，matchedPosts 必须是空数组。
                请返回 JSON，格式如下：
                {
                  "answer":"...",
                  "matchedPosts":[
                    {
                      "id":12,
                      "title":"...",
                      "authorName":"...",
                      "createdAt":"yyyy-MM-dd HH:mm:ss",
                      "summary":"...",
                      "reason":"..."
                    }
                  ]
                }
                用户问题：
                """).append(question).append("\n\n帖子候选列表：\n");
        for (Post p : candidates) {
            String author = p.getUser() == null ? "未知用户"
                    : defaultText(p.getUser().getNickname(), p.getUser().getUsername());
            sb.append("- id=").append(p.getId())
                    .append("; title=").append(defaultText(p.getSummary(), truncate(defaultText(p.getContent(), ""), 20)))
                    .append("; authorName=").append(author)
                    .append("; createdAt=").append(formatDateTime(p.getCreatedAt()))
                    .append("; tags=").append(defaultText(p.getTags(), ""))
                    .append("; category=").append(p.getCategory() == null ? "" : defaultText(p.getCategory().getName(), ""))
                    .append("; content=").append(defaultText(p.getContent(), ""))
                    .append("\n");
        }
        return sb.toString();
    }

    private String truncate(String text, int len) {
        if (text == null || text.length() <= len) {
            return text;
        }
        return text.substring(0, len) + "...";
    }

    private String defaultText(String value, String fallback) {
        return value == null ? fallback : value;
    }

    private String formatDateTime(java.time.LocalDateTime value) {
        if (value == null) {
            return "";
        }
        return value.toString().replace('T', ' ');
    }
}
