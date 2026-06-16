package com.campus.campus_backend.controller.ai;

import com.campus.campus_backend.common.ErrorCode;
import com.campus.campus_backend.common.Result;
import com.campus.campus_backend.dto.ai.NoticeAiQueryRequest;
import com.campus.campus_backend.dto.ai.NoticeAiQueryResponse;
import com.campus.campus_backend.dto.ai.NoticeCategoryRequest;
import com.campus.campus_backend.dto.ai.NoticeCategoryResponse;
import com.campus.campus_backend.service.ai.CampusNoticeAiService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai/notices")
public class AiNoticeController {
    private final CampusNoticeAiService campusNoticeAiService;

    public AiNoticeController(CampusNoticeAiService campusNoticeAiService) {
        this.campusNoticeAiService = campusNoticeAiService;
    }

    @PostMapping("/query")
    public Result<NoticeAiQueryResponse> query(@Valid @RequestBody NoticeAiQueryRequest request) {
        try {
            return Result.ok(campusNoticeAiService.queryNotices(request.getQuestion()));
        } catch (Exception ex) {
            return Result.fail(ErrorCode.SERVER_ERROR, ex.getMessage());
        }
    }

    @PostMapping("/{noticeId}/classify")
    public Result<NoticeCategoryResponse> classify(@PathVariable Long noticeId,
                                                   @RequestBody(required = false) NoticeCategoryRequest request) {
        try {
            boolean force = request != null && request.isForce();
            return Result.ok(campusNoticeAiService.classifyNotice(noticeId, force));
        } catch (Exception ex) {
            return Result.fail(ErrorCode.SERVER_ERROR, ex.getMessage());
        }
    }

    @PostMapping("/classify/batch")
    public Result<Map<String, Object>> classifyBatch(@RequestParam(defaultValue = "20") int limit,
                                                     @RequestParam(defaultValue = "true") boolean onlyUncategorized) {
        try {
            List<NoticeCategoryResponse> results = campusNoticeAiService.classifyBatch(limit, onlyUncategorized);
            long success = results.stream().filter(x -> !"其他".equals(x.getCategory())
                    || (x.getReason() != null && !x.getReason().contains("失败"))).count();
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("total", results.size());
            data.put("success", success);
            data.put("failed", results.size() - success);
            data.put("results", results);
            return Result.ok(data);
        } catch (Exception ex) {
            return Result.fail(ErrorCode.SERVER_ERROR, ex.getMessage());
        }
    }
}
