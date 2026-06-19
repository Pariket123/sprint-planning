package com.sprinklr.sprintplanning.planning.dto;

import com.sprinklr.sprintplanning.common.enums.Domain;
import com.sprinklr.sprintplanning.common.enums.StatusCategory;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Planned issue with latest Jira state and rollover metadata")
public record PlannedIssueViewDto(
    String key,
    String summary,
    String issueType,
    String status,
    StatusCategory statusCategory,
    Double storyPoints,
    Domain domain,
    Long plannedSprintId,
    Long currentSprintId,
    boolean rolledOver
) {
}
