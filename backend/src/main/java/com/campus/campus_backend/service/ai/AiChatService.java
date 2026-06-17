package com.campus.campus_backend.service.ai;

import com.campus.campus_backend.common.BizException;
import com.campus.campus_backend.common.ErrorCode;
import com.campus.campus_backend.domain.*;
import com.campus.campus_backend.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class AiChatService {
    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final AiChatSessionRepository aiChatSessionRepository;
    private final AiChatMessageRepository aiChatMessageRepository;
    private final AiRetrievedSourceRepository aiRetrievedSourceRepository;
    private final UserRepository userRepository;
    private final AiRagService aiRagService;
    private final AiPromptBuilder aiPromptBuilder;
    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;

    public AiChatService(AiChatSessionRepository aiChatSessionRepository,
                         AiChatMessageRepository aiChatMessageRepository,
                         AiRetrievedSourceRepository aiRetrievedSourceRepository,
                         UserRepository userRepository,
                         AiRagService aiRagService,
                         AiPromptBuilder aiPromptBuilder,
                         LlmClient llmClient,
                         ObjectMapper objectMapper) {
        this.aiChatSessionRepository = aiChatSessionRepository;
        this.aiChatMessageRepository = aiChatMessageRepository;
        this.aiRetrievedSourceRepository = aiRetrievedSourceRepository;
        this.userRepository = userRepository;
        this.aiRagService = aiRagService;
        this.aiPromptBuilder = aiPromptBuilder;
        this.llmClient = llmClient;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Map<String, Object> createSession(String title, UserDetails principal) {
        User user = requireUser(principal);
        AiChatSession session = new AiChatSession();
        session.setUser(user);
        session.setTitle(normalizeTitle(title, "新会话"));
        session = aiChatSessionRepository.save(session);
        return buildSessionItem(session, null);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listSessions(UserDetails principal) {
        User user = requireUser(principal);
        List<AiChatSession> sessions = aiChatSessionRepository.findByUserIdOrderByUpdatedAtDesc(user.getId());
        List<Map<String, Object>> result = new ArrayList<>();
        for (AiChatSession session : sessions) {
            List<AiChatMessage> messages = aiChatMessageRepository.findTop12BySessionIdOrderByCreatedAtDesc(session.getId());
            String preview = messages.isEmpty() ? null : messages.get(0).getContent();
            result.add(buildSessionItem(session, preview));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getSessionDetail(Long sessionId, UserDetails principal) {
        User user = requireUser(principal);
        AiChatSession session = getSessionOrThrow(sessionId, user.getId());
        Map<String, Object> data = buildSessionItem(session, null);
        List<AiChatMessage> messages = aiChatMessageRepository.findBySessionIdOrderByCreatedAtAsc(session.getId());
        data.put("messages", messages.stream().map(this::buildMessageItem).toList());
        return data;
    }

    @Transactional
    public void deleteSession(Long sessionId, UserDetails principal) {
        User user = requireUser(principal);
        AiChatSession session = getSessionOrThrow(sessionId, user.getId());
        aiRetrievedSourceRepository.deleteByMessageSessionId(sessionId);
        aiChatMessageRepository.deleteBySessionId(sessionId);
        aiChatSessionRepository.delete(session);
    }

    @Transactional
    public Map<String, Object> sendMessage(Long sessionId, String content, UserDetails principal) {
        User user = requireUser(principal);
        String question = requireContent(content);
        AiChatSession session = sessionId == null
                ? createSessionEntity(user, question)
                : getSessionOrThrow(sessionId, user.getId());

        if (isDefaultTitle(session.getTitle())) {
            session.setTitle(buildTitleFromQuestion(question));
            aiChatSessionRepository.save(session);
        }

        AiChatMessage userMessage = new AiChatMessage();
        userMessage.setSession(session);
        userMessage.setRole(AiChatRole.USER);
        userMessage.setContent(question);
        aiChatMessageRepository.save(userMessage);

        List<AiRagCandidate> candidates = aiRagService.retrieve(question);
        List<AiChatMessage> history = aiChatMessageRepository.findTop12BySessionIdOrderByCreatedAtDesc(session.getId());
        Collections.reverse(history);
        String prompt = aiPromptBuilder.buildAiChatPrompt(question, history, candidates);
        String answer = "平台内暂未检索到足够信息。你可以换个问法，或点击下方相关来源查看详情。";
        List<AiRagCandidate> citedCandidates = candidates;

        try {
            String llmOutput = llmClient.chatJson(prompt);
            ParsedAnswer parsedAnswer = parseAnswer(llmOutput, candidates);
            answer = parsedAnswer.answer();
            citedCandidates = parsedAnswer.sources();
        } catch (Exception ignored) {
            if (candidates.isEmpty()) {
                answer = "平台内暂未检索到足够信息。你可以尝试补充课程名、帖子主题或通知关键词后再问一次。";
            } else {
                answer = "我已经检索到一些相关内容，但本次生成回答失败。你可以先查看下方来源卡片，或稍后重试。";
            }
        }

        AiChatMessage assistantMessage = new AiChatMessage();
        assistantMessage.setSession(session);
        assistantMessage.setRole(AiChatRole.ASSISTANT);
        assistantMessage.setContent(answer);
        assistantMessage = aiChatMessageRepository.save(assistantMessage);
        session.setUpdatedAt(LocalDateTime.now());
        aiChatSessionRepository.save(session);

        List<Map<String, Object>> sourceViews = new ArrayList<>();
        for (AiRagCandidate candidate : citedCandidates.stream().limit(5).toList()) {
            AiRetrievedSource source = new AiRetrievedSource();
            source.setMessage(assistantMessage);
            source.setSourceType(candidate.getSourceType());
            source.setSourceId(candidate.getSourceId());
            source.setTitle(candidate.getTitle());
            source.setSummary(candidate.getSummary());
            source.setScore(candidate.getScore());
            source.setPublishedAt(candidate.getPublishedAt());
            source.setSourceRoute(candidate.getRoute());
            source = aiRetrievedSourceRepository.save(source);
            sourceViews.add(buildSourceItem(source));
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("sessionId", session.getId());
        data.put("messageId", assistantMessage.getId());
        data.put("answer", answer);
        data.put("sources", sourceViews);
        data.put("sessionTitle", session.getTitle());
        return data;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> retrieveDebug(String query, UserDetails principal) {
        requireUser(principal);
        return aiRagService.buildDebugPayload(requireContent(query));
    }

    private ParsedAnswer parseAnswer(String llmOutput, List<AiRagCandidate> candidates) throws Exception {
        JsonNode root = objectMapper.readTree(llmOutput);
        String answer = root.path("answer").asText("").trim();
        if (answer.isEmpty()) {
            answer = "平台内暂未检索到足够信息。";
        }
        List<AiRagCandidate> selected = new ArrayList<>();
        JsonNode citations = root.path("citations");
        if (citations.isArray()) {
            for (JsonNode citation : citations) {
                int index = citation.asInt(-1);
                if (index >= 1 && index <= candidates.size()) {
                    selected.add(candidates.get(index - 1));
                }
            }
        }
        if (selected.isEmpty()) {
            selected = candidates.stream().limit(5).toList();
        } else {
            selected = selected.stream().distinct().toList();
        }
        return new ParsedAnswer(answer, selected);
    }

    private Map<String, Object> buildSessionItem(AiChatSession session, String preview) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", session.getId());
        item.put("title", session.getTitle());
        item.put("preview", abbreviate(preview, 60));
        item.put("createdAt", formatTime(session.getCreatedAt()));
        item.put("updatedAt", formatTime(session.getUpdatedAt()));
        return item;
    }

    private Map<String, Object> buildMessageItem(AiChatMessage message) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", message.getId());
        item.put("role", message.getRole().name());
        item.put("content", message.getContent());
        item.put("createdAt", formatTime(message.getCreatedAt()));
        if (message.getRole() == AiChatRole.ASSISTANT) {
            item.put("sources", aiRetrievedSourceRepository.findByMessageIdOrderByScoreDescIdAsc(message.getId())
                    .stream()
                    .map(this::buildSourceItem)
                    .toList());
        } else {
            item.put("sources", List.of());
        }
        return item;
    }

    private Map<String, Object> buildSourceItem(AiRetrievedSource source) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", source.getId());
        item.put("sourceType", source.getSourceType().name());
        item.put("sourceId", source.getSourceId());
        item.put("title", source.getTitle());
        item.put("summary", source.getSummary());
        item.put("score", Math.round(source.getScore() * 100D) / 100D);
        item.put("publishedAt", source.getPublishedAt());
        item.put("route", source.getSourceRoute());
        return item;
    }

    private AiChatSession createSessionEntity(User user, String question) {
        AiChatSession session = new AiChatSession();
        session.setUser(user);
        session.setTitle(buildTitleFromQuestion(question));
        return aiChatSessionRepository.save(session);
    }

    private AiChatSession getSessionOrThrow(Long sessionId, Long userId) {
        return aiChatSessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));
    }

    private User requireUser(UserDetails principal) {
        if (principal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return userRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new BizException(ErrorCode.UNAUTHORIZED));
    }

    private String requireContent(String content) {
        String value = content == null ? "" : content.trim();
        if (value.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "question is required");
        }
        return value;
    }

    private String normalizeTitle(String title, String fallback) {
        String value = title == null ? "" : title.trim();
        return value.isEmpty() ? fallback : abbreviate(value, 120);
    }

    private String buildTitleFromQuestion(String question) {
        return abbreviate(question, 24);
    }

    private boolean isDefaultTitle(String title) {
        return title == null || title.isBlank() || "新会话".equals(title);
    }

    private String formatTime(java.time.LocalDateTime time) {
        if (time == null) {
            return "";
        }
        return time.atZone(ZONE_ID).format(DATE_TIME_FORMATTER);
    }

    private String abbreviate(String text, int maxLength) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String value = text.trim();
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }

    private record ParsedAnswer(String answer, List<AiRagCandidate> sources) {
    }
}
