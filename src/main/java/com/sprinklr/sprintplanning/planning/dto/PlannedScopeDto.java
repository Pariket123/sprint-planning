package com.sprinklr.sprintplanning.planning.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "Stored planned scope for a sprint")
public record PlannedScopeDto(
    List<String> plannedIssueKeys,
    Instant plannedScopeCapturedAt
) {
}
