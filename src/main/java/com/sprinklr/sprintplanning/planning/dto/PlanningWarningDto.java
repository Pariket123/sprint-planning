package com.sprinklr.sprintplanning.planning.dto;

import com.sprinklr.sprintplanning.common.enums.Domain;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Planning validation warning")
public record PlanningWarningDto(
    PlanningWarningCode code,
    String message,
    Domain domain
) {
}
