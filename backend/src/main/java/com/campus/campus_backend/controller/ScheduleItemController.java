package com.campus.campus_backend.controller;

import com.campus.campus_backend.common.BizException;
import com.campus.campus_backend.common.ErrorCode;
import com.campus.campus_backend.common.Result;
import com.campus.campus_backend.domain.User;
import com.campus.campus_backend.dto.info.ScheduleItemRequest;
import com.campus.campus_backend.repository.UserRepository;
import com.campus.campus_backend.service.info.ScheduleItemService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/schedule-items")
public class ScheduleItemController {
    private final ScheduleItemService scheduleItemService;
    private final UserRepository userRepository;

    public ScheduleItemController(ScheduleItemService scheduleItemService, UserRepository userRepository) {
        this.scheduleItemService = scheduleItemService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public Result<Object> list(@RequestParam(required = false) String filter,
            @AuthenticationPrincipal UserDetails principal) {
        return Result.ok(scheduleItemService.list(requireUser(principal), filter));
    }

    @PostMapping
    public Result<Object> create(@Valid @RequestBody ScheduleItemRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        return Result.ok(scheduleItemService.create(requireUser(principal), request));
    }

    @GetMapping("/{id}")
    public Result<Object> detail(@PathVariable Long id, @AuthenticationPrincipal UserDetails principal) {
        return Result.ok(scheduleItemService.detail(id, requireUser(principal)));
    }

    @PutMapping("/{id}")
    public Result<Object> update(@PathVariable Long id, @Valid @RequestBody ScheduleItemRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        return Result.ok(scheduleItemService.update(id, requireUser(principal), request));
    }

    @DeleteMapping("/{id}")
    public Result<Object> delete(@PathVariable Long id, @AuthenticationPrincipal UserDetails principal) {
        scheduleItemService.delete(id, requireUser(principal));
        return Result.okMessage("删除成功");
    }

    @PostMapping("/{id}/complete")
    public Result<Object> complete(@PathVariable Long id, @AuthenticationPrincipal UserDetails principal) {
        return Result.ok(scheduleItemService.complete(id, requireUser(principal)));
    }

    @DeleteMapping("/{id}/complete")
    public Result<Object> uncomplete(@PathVariable Long id, @AuthenticationPrincipal UserDetails principal) {
        return Result.ok(scheduleItemService.uncomplete(id, requireUser(principal)));
    }

    private User requireUser(UserDetails principal) {
        if (principal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return userRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new BizException(ErrorCode.UNAUTHORIZED));
    }
}
