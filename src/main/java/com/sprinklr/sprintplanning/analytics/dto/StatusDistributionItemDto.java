package com.sprinklr.sprintplanning.analytics.dto;

import com.sprinklr.sprintplanning.common.enums.StatusCategory;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Issue distribution for a Jira status")
public record StatusDistributionItemDto(
    String status,
    StatusCategory statusCategory,
    int count,
    double storyPoints
) {
}
