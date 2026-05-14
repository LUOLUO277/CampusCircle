package com.campus.campus_backend.controller;

import com.campus.campus_backend.common.BizException;
import com.campus.campus_backend.common.ErrorCode;
import com.campus.campus_backend.common.Result;
import com.campus.campus_backend.domain.User;
import com.campus.campus_backend.dto.info.ManualNoticeRequest;
import com.campus.campus_backend.dto.info.UpsertInfoSourceRequest;
import com.campus.campus_backend.repository.UserRepository;
import com.campus.campus_backend.service.info.InfoCenterService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminInfoCenterController {
    private final InfoCenterService infoCenterService;
    private final UserRepository userRepository;

    public AdminInfoCenterController(InfoCenterService infoCenterService, UserRepository userRepository) {
        this.infoCenterService = infoCenterService;
        this.userRepository = userRepository;
    }

    @PostMapping("/info-sources")
    public Result<Object> createSource(@Valid @RequestBody UpsertInfoSourceRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        ensureAdmin(principal);
        return Result.ok(infoCenterService.saveSource(null, request));
    }

    @PutMapping("/info-sources/{id}")
    public Result<Object> updateSource(@PathVariable Long id,
            @Valid @RequestBody UpsertInfoSourceRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        ensureAdmin(principal);
        return Result.ok(infoCenterService.saveSource(id, request));
    }

    @GetMapping("/info-sources/{id}")
    public Result<Object> getSourceDetail(@PathVariable Long id, @AuthenticationPrincipal UserDetails principal) {
        ensureAdmin(principal);
        return Result.ok(infoCenterService.getSourceDetail(id));
    }

    @PostMapping("/info-sources/{id}/enable")
    public Result<Object> enableSource(@PathVariable Long id, @AuthenticationPrincipal UserDetails principal) {
        ensureAdmin(principal);
        return Result.ok(infoCenterService.toggleSourceStatus(id, "ACTIVE"));
    }

    @PostMapping("/info-sources/{id}/disable")
    public Result<Object> disableSource(@PathVariable Long id, @AuthenticationPrincipal UserDetails principal) {
        ensureAdmin(principal);
        return Result.ok(infoCenterService.toggleSourceStatus(id, "DISABLED"));
    }

    @PostMapping("/info-center/notices/manual")
    public Result<Object> createManualNotice(@Valid @RequestBody ManualNoticeRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        ensureAdmin(principal);
        return Result.ok(infoCenterService.manualNotice(null, request));
    }

    @PutMapping("/info-center/notices/{id}")
    public Result<Object> updateManualNotice(@PathVariable Long id,
            @Valid @RequestBody ManualNoticeRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        ensureAdmin(principal);
        return Result.ok(infoCenterService.updateManualNotice(id, request));
    }

    @PostMapping("/info-center/notices/{id}/offline")
    public Result<Object> offlineNotice(@PathVariable Long id, @AuthenticationPrincipal UserDetails principal) {
        ensureAdmin(principal);
        return Result.ok(infoCenterService.offlineNotice(id));
    }

    @PostMapping("/info-sources/{id}/fetch")
    public Result<Object> fetchSource(@PathVariable Long id, @AuthenticationPrincipal UserDetails principal) {
        ensureAdmin(principal);
        return Result.ok(infoCenterService.fetchSource(id));
    }

    @PostMapping("/info-center/purge-mock")
    public Result<Object> purgeMock(@AuthenticationPrincipal UserDetails principal) {
        ensureAdmin(principal);
        return Result.ok(infoCenterService.purgeMockData());
    }

    @GetMapping("/info-sources/{id}/fetch-logs")
    public Result<Object> fetchLogs(@PathVariable Long id, @AuthenticationPrincipal UserDetails principal) {
        ensureAdmin(principal);
        return Result.ok(infoCenterService.fetchLogs(id));
    }

    private void ensureAdmin(UserDetails principal) {
        if (principal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        User user = userRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new BizException(ErrorCode.UNAUTHORIZED));
        if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
    }
}
