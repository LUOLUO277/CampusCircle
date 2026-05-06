package com.campus.campus_backend.dto.info;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UpdateSubscriptionKeywordsRequest {
    private List<String> keywords;
}
