package com.sprinklr.sprintplanning.planning.dto;

import com.sprinklr.sprintplanning.planning.model.CapacityAllocationPercents;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "Request to upsert capacity allocation percentages")
public record UpdateCapacityAllocationRequest(
    @NotNull @Valid List<CapacityAllocationPercents> capacityAllocation
) {
}
