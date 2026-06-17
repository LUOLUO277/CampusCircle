package com.campus.campus_backend.dto.info;

import com.campus.campus_backend.domain.ScheduleItemType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScheduleItemRequest {
    @NotBlank(message = "标题不能为空")
    private String title;

    private String description;

    @NotBlank(message = "截止时间不能为空")
    private String deadlineAt;

    @NotNull(message = "日程类型不能为空")
    private ScheduleItemType type;

    private Long relatedNoticeId;
}
