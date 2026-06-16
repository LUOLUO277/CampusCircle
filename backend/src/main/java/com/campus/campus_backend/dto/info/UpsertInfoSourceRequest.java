package com.campus.campus_backend.dto.info;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpsertInfoSourceRequest {
    @NotBlank
    @Size(max = 100)
    private String name;
    @NotBlank
    @Size(max = 20)
    private String type;
    @NotBlank
    @Size(max = 100)
    private String sourceKey;
    @Size(max = 500)
    private String sourceUrl;
    @Size(max = 500)
    private String searchKeywords;
    @Size(max = 50)
    private String fetchStrategy;
    @Size(max = 20000)
    private String fetchConfigJson;
    @Size(max = 20)
    private String status;
}
