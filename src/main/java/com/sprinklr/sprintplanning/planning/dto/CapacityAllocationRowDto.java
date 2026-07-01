package com.sprinklr.sprintplanning.planning.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Computed capacity split for a domain or total")
public record CapacityAllocationRowDto(
    String key,
    String label,
    double availableStoryPoints,
    double roadmapPercent,
    double bugSupportPercent,
    double plannedRoadmapStoryPoints,
    double plannedBugSupportStoryPoints
) {
}
