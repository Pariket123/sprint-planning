package com.sprinklr.sprintplanning.planning.dto;

import com.sprinklr.sprintplanning.planning.model.DomainCapacity;
import com.sprinklr.sprintplanning.planning.model.LeaveEntry;
import com.sprinklr.sprintplanning.planning.model.PlanningOverride;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Schema(description = "Persisted sprint planning inputs")
public record PlanningDataDto(
    String id,
    String podId,
    Long jiraSprintId,
    List<DomainCapacity> capacity,
    List<LeaveEntry> leaves,
    List<PlanningOverride> overrides,
    Map<String, Double> rolloverStoryPoints,
    Instant updatedAt
) {
}
