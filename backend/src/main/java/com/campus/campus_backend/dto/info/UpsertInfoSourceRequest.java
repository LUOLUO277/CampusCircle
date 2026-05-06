package com.campus.campus_backend.dto.info;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpsertInfoSourceRequest {
    @NotBlank
    private String name;
    @NotBlank
    private String type;
    @NotBlank
    private String sourceKey;
    private String sourceUrl;
    private String searchKeywords;
    private String fetchStrategy;
    private String fetchConfigJson;
    private String status;
}
