package com.campus.campus_backend.controller.ai;

import com.campus.campus_backend.common.Result;
import com.campus.campus_backend.dto.ai.AiChatAskRequest;
import com.campus.campus_backend.dto.ai.AiChatCreateSessionRequest;
import com.campus.campus_backend.dto.ai.AiChatSendMessageRequest;
import com.campus.campus_backend.service.ai.AiChatService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai-chat")
public class AiChatController {
    private final AiChatService aiChatService;

    public AiChatController(AiChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    @PostMapping("/sessions")
    public Result<Object> createSession(@RequestBody(required = false) AiChatCreateSessionRequest request,
                                        @AuthenticationPrincipal UserDetails principal) {
        return Result.ok(aiChatService.createSession(request == null ? null : request.getTitle(), principal));
    }

    @GetMapping("/sessions")
    public Result<Object> listSessions(@AuthenticationPrincipal UserDetails principal) {
        return Result.ok(aiChatService.listSessions(principal));
    }

    @GetMapping("/sessions/{sessionId}")
    public Result<Object> getSessionDetail(@PathVariable Long sessionId,
                                           @AuthenticationPrincipal UserDetails principal) {
        return Result.ok(aiChatService.getSessionDetail(sessionId, principal));
    }

    @DeleteMapping("/sessions/{sessionId}")
    public Result<Object> deleteSession(@PathVariable Long sessionId,
                                        @AuthenticationPrincipal UserDetails principal) {
        aiChatService.deleteSession(sessionId, principal);
        return Result.okMessage("删除成功");
    }

    @PostMapping("/sessions/{sessionId}/messages")
    public Result<Object> sendMessage(@PathVariable Long sessionId,
                                      @Valid @RequestBody AiChatSendMessageRequest request,
                                      @AuthenticationPrincipal UserDetails principal) {
        return Result.ok(aiChatService.sendMessage(sessionId, request.getContent(), principal));
    }

    @PostMapping("/ask")
    public Result<Object> ask(@Valid @RequestBody AiChatAskRequest request,
                              @AuthenticationPrincipal UserDetails principal) {
        return Result.ok(aiChatService.sendMessage(request.getSessionId(), request.getQuestion(), principal));
    }

    @GetMapping("/retrieve")
    public Result<Object> retrieve(@RequestParam String query,
                                   @AuthenticationPrincipal UserDetails principal) {
        return Result.ok(aiChatService.retrieveDebug(query, principal));
    }
}
