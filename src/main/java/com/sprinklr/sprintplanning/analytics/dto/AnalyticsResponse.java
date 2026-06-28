package com.sprinklr.sprintplanning.analytics.dto;

import com.sprinklr.sprintplanning.analytics.dto.DevSubDomainMetricsDto;
import com.sprinklr.sprintplanning.analytics.dto.WorkflowStageDistributionDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Sprint analytics insight payload")
public record AnalyticsResponse(
    Long jiraSprintId,
    String sprintName,
    double totalStoryPoints,
    double completedStoryPoints,
    double remainingStoryPoints,
    IssueCountsDto issueCounts,
    BugsVsFeaturesDto bugsVsFeatures,
    List<StatusDistributionItemDto> statusDistribution,
    List<DomainBreakdownItemDto> domainBreakdown,
    WorkflowStageDistributionDto workflowStageDistribution,
    DevSubDomainMetricsDto devSubDomainMetrics
) {
}
