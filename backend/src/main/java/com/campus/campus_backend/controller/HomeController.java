package com.campus.campus_backend.controller;

import com.campus.campus_backend.common.BizException;
import com.campus.campus_backend.common.ErrorCode;
import com.campus.campus_backend.common.Result;
import com.campus.campus_backend.domain.User;
import com.campus.campus_backend.repository.UserRepository;
import com.campus.campus_backend.service.info.DeadlineReminderService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/home")
public class HomeController {
    private final DeadlineReminderService deadlineReminderService;
    private final UserRepository userRepository;

    public HomeController(DeadlineReminderService deadlineReminderService, UserRepository userRepository) {
        this.deadlineReminderService = deadlineReminderService;
        this.userRepository = userRepository;
    }

    @GetMapping("/deadline-reminders")
    public Result<Object> deadlineReminders(@RequestParam(defaultValue = "7") int days,
            @AuthenticationPrincipal UserDetails principal) {
        return Result.ok(deadlineReminderService.getReminders(optionalUser(principal), days));
    }

    private User optionalUser(UserDetails principal) {
        if (principal == null) {
            return null;
        }
        return userRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new BizException(ErrorCode.UNAUTHORIZED));
    }
}
