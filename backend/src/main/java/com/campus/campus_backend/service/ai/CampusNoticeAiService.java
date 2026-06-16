package com.campus.campus_backend.service.ai;

import com.campus.campus_backend.domain.AggregatedNotice;
import com.campus.campus_backend.dto.ai.NoticeAiQueryResponse;
import com.campus.campus_backend.dto.ai.NoticeCategoryResponse;
import com.campus.campus_backend.repository.AggregatedNoticeRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.PageRequest;

@Service
public class CampusNoticeAiService {
    private static final Set<String> ALLOWED_CATEGORIES = Set.of("课程作业", "考试相关", "竞赛活动", "生活服务", "其他");
    private final AggregatedNoticeRepository noticeRepository;
    private final AiPromptBuilder promptBuilder;
    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;

    public CampusNoticeAiService(AggregatedNoticeRepository noticeRepository,
                                 AiPromptBuilder promptBuilder,
                                 LlmClient llmClient,
                                 ObjectMapper objectMapper) {
        this.noticeRepository = noticeRepository;
        this.promptBuilder = promptBuilder;
        this.llmClient = llmClient;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public NoticeAiQueryResponse queryNotices(String question) {
        LocalDateTime start = LocalDateTime.now().minusDays(30);
        List<AggregatedNotice> candidates = noticeRepository.findRecentForAi("ONLINE", start, PageRequest.of(0, 50));
        if (candidates.isEmpty()) {
            NoticeAiQueryResponse res = new NoticeAiQueryResponse();
            res.setAnswer("未找到相关通知");
            return res;
        }
        String prompt = promptBuilder.buildNoticeQueryPrompt(question, candidates);
        String llmOutput = llmClient.chatJson(prompt);
        return parseNoticeQueryResponse(llmOutput, candidates);
    }

    @Transactional
    public NoticeCategoryResponse classifyNotice(Long noticeId, boolean force) {
        AggregatedNotice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new IllegalArgumentException("通知不存在"));
        if (!force && ALLOWED_CATEGORIES.contains(notice.getCategory())) {
            NoticeCategoryResponse cached = new NoticeCategoryResponse();
            cached.setNoticeId(noticeId);
            cached.setCategory(notice.getCategory());
            cached.setReason("已存在分类，返回已有结果");
            return cached;
        }
        String prompt = promptBuilder.buildNoticeCategoryPrompt(notice);
        String llmOutput = llmClient.chatJson(prompt);
        NoticeCategoryResponse parsed = parseNoticeCategoryResponse(noticeId, llmOutput);
        notice.setCategory(parsed.getCategory());
        noticeRepository.save(notice);
        return parsed;
    }

    @Transactional
    public List<NoticeCategoryResponse> classifyBatch(int limit, boolean onlyUncategorized) {
        List<AggregatedNotice> targets = onlyUncategorized
                ? noticeRepository.findTopNForAiClassify("ONLINE", limit)
                : noticeRepository.findTopNByStatusOrderByPublishTimeDescCreatedAtDesc("ONLINE", limit);
        List<NoticeCategoryResponse> results = new ArrayList<>();
        for (AggregatedNotice n : targets) {
            try {
                results.add(classifyNotice(n.getId(), true));
            } catch (Exception ex) {
                NoticeCategoryResponse failed = new NoticeCategoryResponse();
                failed.setNoticeId(n.getId());
                failed.setCategory("其他");
                failed.setReason("分类失败，已回退为其他");
                n.setCategory("其他");
                noticeRepository.save(n);
                results.add(failed);
            }
        }
        return results;
    }

    private NoticeAiQueryResponse parseNoticeQueryResponse(String json, List<AggregatedNotice> candidates) {
        try {
            JsonNode root = objectMapper.readTree(json);
            NoticeAiQueryResponse response = new NoticeAiQueryResponse();
            response.setAnswer(root.path("answer").asText("未找到相关通知"));
            JsonNode matched = root.path("matchedNotices");
            if (matched.isArray()) {
                for (JsonNode node : matched) {
                    NoticeAiQueryResponse.MatchedNotice item = new NoticeAiQueryResponse.MatchedNotice();
                    item.setId(node.path("id").asLong());
                    item.setTitle(node.path("title").asText(""));
                    item.setPublishTime(node.path("publishTime").asText(""));
                    item.setCategory(normalizeCategory(node.path("category").asText("其他")));
                    item.setSummary(node.path("summary").asText(""));
                    item.setReason(node.path("reason").asText(""));
                    if (existsInCandidates(item.getId(), candidates)) {
                        response.getMatchedNotices().add(item);
                    }
                }
            }
            if (response.getMatchedNotices().isEmpty() && !"未找到相关通知".equals(response.getAnswer())) {
                response.setAnswer("未找到相关通知");
            }
            return response;
        } catch (Exception ex) {
            NoticeAiQueryResponse fallback = new NoticeAiQueryResponse();
            fallback.setAnswer("未找到相关通知");
            return fallback;
        }
    }

    private NoticeCategoryResponse parseNoticeCategoryResponse(Long noticeId, String json) {
        NoticeCategoryResponse response = new NoticeCategoryResponse();
        response.setNoticeId(noticeId);
        try {
            JsonNode root = objectMapper.readTree(json);
            response.setCategory(normalizeCategory(root.path("category").asText("其他")));
            response.setReason(root.path("reason").asText("模型判断不确定，归为其他"));
            return response;
        } catch (Exception ex) {
            response.setCategory("其他");
            response.setReason("解析模型输出失败，归为其他");
            return response;
        }
    }

    private String normalizeCategory(String raw) {
        if (ALLOWED_CATEGORIES.contains(raw)) {
            return raw;
        }
        return "其他";
    }

    private boolean existsInCandidates(Long id, List<AggregatedNotice> candidates) {
        for (AggregatedNotice n : candidates) {
            if (n.getId().equals(id)) {
                return true;
            }
        }
        return false;
    }
}
