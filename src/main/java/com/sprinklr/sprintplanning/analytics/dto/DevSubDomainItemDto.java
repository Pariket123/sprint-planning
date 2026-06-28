package com.sprinklr.sprintplanning.analytics.dto;

import com.sprinklr.sprintplanning.common.enums.Domain;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Completion metrics for an engineering sub-domain within dev-or-beyond pool")
public record DevSubDomainItemDto(
    Domain domain,
    int applicableIssueCount,
    int completedIssueCount,
    double issueCompletionRatio,
    double totalStoryPoints,
    double completedStoryPoints,
    double storyPointCompletionRatio
) {
}
