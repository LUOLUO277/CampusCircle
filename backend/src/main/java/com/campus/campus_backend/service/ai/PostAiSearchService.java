package com.campus.campus_backend.service.ai;

import com.campus.campus_backend.domain.Post;
import com.campus.campus_backend.dto.ai.PostAiSearchResponse;
import com.campus.campus_backend.repository.PostRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PostAiSearchService {
    private final PostRepository postRepository;
    private final AiPromptBuilder promptBuilder;
    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;

    public PostAiSearchService(PostRepository postRepository,
                               AiPromptBuilder promptBuilder,
                               LlmClient llmClient,
                               ObjectMapper objectMapper) {
        this.postRepository = postRepository;
        this.promptBuilder = promptBuilder;
        this.llmClient = llmClient;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public PostAiSearchResponse search(String question) {
        LocalDateTime start = LocalDateTime.now().minusDays(15);
        List<Post> candidates = postRepository.findRecentForAi(start, PageRequest.of(0, 50));
        if (candidates.isEmpty()) {
            PostAiSearchResponse res = new PostAiSearchResponse();
            res.setAnswer("未找到相关帖子");
            return res;
        }
        String prompt = promptBuilder.buildPostSearchPrompt(question, candidates);
        String llmOutput = llmClient.chatJson(prompt);
        return parsePostSearchResponse(llmOutput, candidates);
    }

    private PostAiSearchResponse parsePostSearchResponse(String json, List<Post> candidates) {
        try {
            JsonNode root = objectMapper.readTree(json);
            PostAiSearchResponse response = new PostAiSearchResponse();
            response.setAnswer(root.path("answer").asText("未找到相关帖子"));
            JsonNode matched = root.path("matchedPosts");
            if (matched.isArray()) {
                for (JsonNode node : matched) {
                    Long id = node.path("id").asLong();
                    if (!existsInCandidates(id, candidates)) {
                        continue;
                    }
                    PostAiSearchResponse.MatchedPost item = new PostAiSearchResponse.MatchedPost();
                    item.setId(id);
                    item.setTitle(node.path("title").asText(""));
                    item.setAuthorName(node.path("authorName").asText(""));
                    item.setCreatedAt(node.path("createdAt").asText(""));
                    item.setSummary(node.path("summary").asText(""));
                    item.setReason(node.path("reason").asText(""));
                    response.getMatchedPosts().add(item);
                }
            }
            if (response.getMatchedPosts().isEmpty() && !"未找到相关帖子".equals(response.getAnswer())) {
                response.setAnswer("未找到相关帖子");
            }
            return response;
        } catch (Exception ex) {
            PostAiSearchResponse fallback = new PostAiSearchResponse();
            fallback.setAnswer("未找到相关帖子");
            return fallback;
        }
    }

    private boolean existsInCandidates(Long id, List<Post> candidates) {
        for (Post p : candidates) {
            if (p.getId().equals(id)) {
                return true;
            }
        }
        return false;
    }
}
