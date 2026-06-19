package com.sprinklr.sprintplanning.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Issue count summary")
public record IssueCountsDto(
    int total,
    int completed,
    int remaining
) {
}
