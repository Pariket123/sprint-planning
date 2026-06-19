package com.sprinklr.sprintplanning.planning.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "Save planned issue keys for a sprint")
public record UpdatePlannedScopeRequest(
    @NotNull List<String> plannedIssueKeys
) {
}
