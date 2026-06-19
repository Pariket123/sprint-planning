package com.sprinklr.sprintplanning.team.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Pod summary")
public record PodResponse(
    @Schema(example = "64f1a2b3c4d5e6f7a8b9c0d2") String id,
    @Schema(example = "64f1a2b3c4d5e6f7a8b9c0d1") String teamId,
    @Schema(example = "FORECASTING") String code,
    @Schema(example = "Forecasting") String name,
    boolean active,
    PodJiraConfigSummary jiraConfig
) {
}
