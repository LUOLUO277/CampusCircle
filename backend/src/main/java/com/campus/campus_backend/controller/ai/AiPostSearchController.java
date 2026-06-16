package com.campus.campus_backend.controller.ai;

import com.campus.campus_backend.common.ErrorCode;
import com.campus.campus_backend.common.Result;
import com.campus.campus_backend.dto.ai.PostAiSearchRequest;
import com.campus.campus_backend.dto.ai.PostAiSearchResponse;
import com.campus.campus_backend.service.ai.PostAiSearchService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai/posts")
public class AiPostSearchController {
    private final PostAiSearchService postAiSearchService;

    public AiPostSearchController(PostAiSearchService postAiSearchService) {
        this.postAiSearchService = postAiSearchService;
    }

    @PostMapping("/search")
    public Result<PostAiSearchResponse> search(@Valid @RequestBody PostAiSearchRequest request) {
        try {
            return Result.ok(postAiSearchService.search(request.getQuestion()));
        } catch (Exception ex) {
            return Result.fail(ErrorCode.SERVER_ERROR, ex.getMessage());
        }
    }
}
