package com.sprinklr.sprintplanning.release.dto;

import com.sprinklr.sprintplanning.planning.dto.CapacityAllocationTableDto;
import com.sprinklr.sprintplanning.planning.dto.DomainPlanningMetricsDto;
import com.sprinklr.sprintplanning.planning.dto.RiskLevel;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Computed release capacity metrics for filtered release issues")
public record ReleaseCapacitySummaryDto(
    String releaseId,
    Integer durationDays,
    double totalAvailableCapacity,
    double totalCommittedStoryPoints,
    int totalIssueCount,
    List<DomainPlanningMetricsDto> domainMetrics,
    RiskLevel riskLevel,
    CapacityAllocationTableDto capacityAllocationTable
) {
}
