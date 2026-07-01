package com.sprinklr.sprintplanning.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Issue count and story points for a single issue type")
public record IssueTypeMetricsDto(
    String issueType,
    int count,
    double storyPoints
) {
}
