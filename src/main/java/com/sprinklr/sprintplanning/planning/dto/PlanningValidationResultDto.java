package com.sprinklr.sprintplanning.planning.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Planning validation result")
public record PlanningValidationResultDto(
    List<PlanningWarningDto> warnings,
    RiskLevel riskLevel
) {
}
