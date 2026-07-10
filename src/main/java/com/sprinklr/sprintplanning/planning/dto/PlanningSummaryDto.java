package com.sprinklr.sprintplanning.planning.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Computed sprint planning summary")
public record PlanningSummaryDto(
    Long jiraSprintId,
    double totalAvailableCapacity,
    double totalRollover,
    double totalSelectedStoryPoints,
    int totalSelectedIssueCount,
    double totalCommittedStoryPoints,
    int totalCommittedIssueCount,
    double totalRoadmapCapacity,
    List<DomainPlanningMetricsDto> domainMetrics,
    RiskLevel riskLevel,
    CapacityAllocationTableDto capacityAllocationTable
) {
}
