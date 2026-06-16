package com.campus.campus_backend.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class LlmClient {
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String model;
    private final String apiKey;

    public LlmClient(
            ObjectMapper objectMapper,
            @Value("${llm.base-url}") String baseUrl,
            @Value("${llm.api-key:}") String apiKeyInProps,
            @Value("${llm.model}") String model) {
        this.objectMapper = objectMapper;
        this.model = model;
        String envApiKey = System.getenv("LLM_API_KEY");
        this.apiKey = envApiKey != null && !envApiKey.isBlank() ? envApiKey : apiKeyInProps;
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public String chatJson(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("LLM API Key 未配置");
        }
        Map<String, Object> req = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", "你是严格JSON输出助手。仅输出JSON字符串。"),
                        Map.of("role", "user", "content", prompt)),
                "temperature", 0.2
        );
        try {
            String body = restClient.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(req)
                    .retrieve()
                    .body(String.class);
            JsonNode root = objectMapper.readTree(body);
            JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
            if (contentNode.isMissingNode() || contentNode.asText().isBlank()) {
                throw new IllegalStateException("LLM响应为空");
            }
            return stripMarkdownCodeFence(contentNode.asText());
        } catch (Exception ex) {
            log.error("LLM 调用失败", ex);
            throw new IllegalStateException("AI 服务暂时不可用，请稍后重试");
        }
    }

    private String stripMarkdownCodeFence(String text) {
        String t = text == null ? "" : text.trim();
        if (t.startsWith("```")) {
            int firstNewLine = t.indexOf('\n');
            if (firstNewLine > 0) {
                t = t.substring(firstNewLine + 1);
            }
            if (t.endsWith("```")) {
                t = t.substring(0, t.length() - 3).trim();
            }
        }
        return t;
    }
}
