package com.campus.campus_backend.dto.info;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpsertCanvasBindingRequest {
    @NotBlank
    private String baseUrl;

    @NotBlank
    private String username;

    private String password;

    private Long[] courseIds;

    private Boolean includeTodo;

    private Boolean includeGlobalAnnouncements;
}
