package com.campus.campus_backend.controller;

import com.campus.campus_backend.common.BizException;
import com.campus.campus_backend.common.ErrorCode;
import com.campus.campus_backend.common.Result;
import com.campus.campus_backend.domain.User;
import com.campus.campus_backend.dto.info.UpsertCanvasBindingRequest;
import com.campus.campus_backend.repository.UserRepository;
import com.campus.campus_backend.service.info.CanvasBindingService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/canvas-binding")
public class CanvasBindingController {
    private static final Logger log = LoggerFactory.getLogger(CanvasBindingController.class);
    private final CanvasBindingService canvasBindingService;
    private final UserRepository userRepository;

    public CanvasBindingController(CanvasBindingService canvasBindingService, UserRepository userRepository) {
        this.canvasBindingService = canvasBindingService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public Result<Object> getBinding(@AuthenticationPrincipal UserDetails principal) {
        return Result.ok(canvasBindingService.getBinding(requireUser(principal)));
    }

    @PutMapping
    public Result<Object> saveBinding(@Valid @RequestBody UpsertCanvasBindingRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        return Result.ok(canvasBindingService.saveBinding(requireUser(principal), request));
    }

    @PostMapping("/sync")
    public Result<Object> sync(
            @RequestParam(value = "source", defaultValue = "all") String source,
            @RequestParam(value = "forceRelogin", defaultValue = "false") boolean forceRelogin,
            @RequestParam(value = "debugRaw", defaultValue = "false") boolean debugRaw,
            @AuthenticationPrincipal UserDetails principal) {
        User user = requireUser(principal);
        log.error("SYNC_ENTRY userId={} source={} forceRelogin={} debugRaw={}",
                user.getId(), source, forceRelogin, debugRaw);
        return Result.ok(canvasBindingService.sync(user, source, forceRelogin, debugRaw));
    }

    @PostMapping("/browser-login")
    public Result<Object> browserLogin(@AuthenticationPrincipal UserDetails principal) {
        return Result.ok(canvasBindingService.browserLogin(requireUser(principal)));
    }

    @DeleteMapping
    public Result<Object> disconnect(@AuthenticationPrincipal UserDetails principal) {
        return Result.ok(canvasBindingService.disconnect(requireUser(principal)));
    }

    private User requireUser(UserDetails principal) {
        if (principal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return userRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new BizException(ErrorCode.UNAUTHORIZED));
    }
}
