package com.campus.campus_backend.service.ai;

import com.campus.campus_backend.domain.AggregatedNotice;
import com.campus.campus_backend.domain.AiChatMessage;
import com.campus.campus_backend.domain.Post;
import org.springframework.stereotype.Component;

import java.util.Collections;
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

    public String buildAiChatPrompt(String question, List<AiChatMessage> history, List<AiRagCandidate> candidates) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
                你是“AI 校园信息助手”，负责基于平台内通知、帖子和课程圈子内容回答学生问题。
                你必须遵守以下规则：
                1. 只能依据给定候选内容回答，不要编造不存在的通知、帖子、课程、老师或截止时间。
                2. 如果候选内容不足以支撑结论，请明确说“平台内暂未检索到足够信息”。
                3. 回答要简洁、分点、可执行，优先总结共识，再补充细节。
                4. 回答末尾如果引用了内容，请用 [1] [2] 这样的编号表示。
                5. 只输出 JSON，不要输出 Markdown 代码块，不要输出额外解释。

                输出 JSON 格式：
                {
                  "answer": "给用户展示的最终回答",
                  "citations": [1, 2]
                }
                """);

        sb.append("\n最近会话上下文：\n");
        List<AiChatMessage> safeHistory = history == null ? Collections.emptyList() : history;
        if (safeHistory.isEmpty()) {
            sb.append("(无)\n");
        } else {
            for (AiChatMessage message : safeHistory.stream().skip(Math.max(0, safeHistory.size() - 8)).toList()) {
                sb.append("- ")
                        .append(message.getRole().name())
                        .append(": ")
                        .append(defaultText(message.getContent(), ""))
                        .append("\n");
            }
        }

        sb.append("\n用户问题：\n").append(question).append("\n");
        sb.append("\n检索到的候选内容：\n");
        if (candidates == null || candidates.isEmpty()) {
            sb.append("(无候选内容)\n");
        } else {
            int index = 1;
            for (AiRagCandidate candidate : candidates) {
                sb.append("[")
                        .append(index++)
                        .append("] 类型: ")
                        .append(candidate.getSourceType().name())
                        .append("\n标题: ")
                        .append(defaultText(candidate.getTitle(), ""))
                        .append("\n摘要: ")
                        .append(defaultText(candidate.getSummary(), ""))
                        .append("\n时间: ")
                        .append(defaultText(candidate.getPublishedAt(), ""))
                        .append("\n详情: ")
                        .append(defaultText(candidate.getContent(), ""))
                        .append("\n\n");
            }
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
