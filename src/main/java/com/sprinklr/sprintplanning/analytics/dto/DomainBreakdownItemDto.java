package com.sprinklr.sprintplanning.analytics.dto;

import com.sprinklr.sprintplanning.common.enums.Domain;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Story points and issue counts per domain with distribution and completion percentages")
public record DomainBreakdownItemDto(
    Domain domain,
    int count,
    double storyPoints,
    double issueCountPercentage,
    double storyPointPercentage,
    int completedIssueCount,
    double completedStoryPoints,
    int remainingIssueCount,
    double remainingStoryPoints,
    double issueCompletionPercentage,
    double storyPointCompletionPercentage
) {
}
