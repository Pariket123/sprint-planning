package com.sprinklr.sprintplanning.planning.dto;

import com.sprinklr.sprintplanning.common.enums.Domain;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Per-domain planning metrics")
public record DomainPlanningMetricsDto(
    Domain domain,
    double availableCapacity,
    double rollover,
    double selectedStoryPoints,
    int selectedIssueCount,
    double suggestedCommitment,
    double committedStoryPoints,
    double utilizationPercent,
    CapacityRiskStatus capacityRisk
) {
}
