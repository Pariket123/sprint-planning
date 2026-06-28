package com.sprinklr.sprintplanning.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Distribution of issues across configured workflow sections")
public record WorkflowStageDistributionDto(
    int totalIssues,
    List<WorkflowStageSectionItemDto> sections
) {
}
