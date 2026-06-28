package com.sprinklr.sprintplanning.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Issue count for a configured workflow analysis section")
public record WorkflowStageSectionItemDto(
    String key,
    String label,
    int count,
    int totalIssues,
    double ratio
) {
}
