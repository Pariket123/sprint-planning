package com.sprinklr.sprintplanning.release.dto;

import com.sprinklr.sprintplanning.planning.model.CapacityAllocationPercents;
import com.sprinklr.sprintplanning.planning.model.PersonCapacity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "Request to upsert team capacity for a release")
public record UpdateReleaseCapacityRequest(
    @NotNull @Valid List<PersonCapacity> capacity,
    @Schema(description = "Leave percentage applied to all domain capacity (0-100)")
    @Min(0) @Max(100) Double leavePercent,
    @Valid List<CapacityAllocationPercents> capacityAllocation
) {
}
