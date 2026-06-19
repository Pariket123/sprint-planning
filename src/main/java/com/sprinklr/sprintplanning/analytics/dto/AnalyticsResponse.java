package com.sprinklr.sprintplanning.analytics.dto;

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
    List<DomainBreakdownItemDto> domainBreakdown
) {
}
