package com.sprinklr.sprintplanning.planning.dto;

import com.sprinklr.sprintplanning.planning.model.PlanningOverride;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "Request to upsert planning overrides")
public record UpdateOverridesRequest(
    @NotNull @Valid List<PlanningOverride> overrides
) {
}
