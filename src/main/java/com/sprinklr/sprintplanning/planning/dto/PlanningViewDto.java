package com.sprinklr.sprintplanning.planning.dto;

import com.sprinklr.sprintplanning.common.model.SprintView;
import com.sprinklr.sprintplanning.planning.dto.RolloverIssueDto;
import com.sprinklr.sprintplanning.planning.model.PersonCapacity;
import com.sprinklr.sprintplanning.planning.model.LeaveEntry;
import com.sprinklr.sprintplanning.planning.model.PlanningOverride;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

@Schema(description = "Merged sprint planning view with live Jira data and persisted inputs")
public record PlanningViewDto(
    Long jiraSprintId,
    SprintView sprint,
    List<PersonCapacity> capacity,
    List<LeaveEntry> leaves,
    List<PlanningOverride> overrides,
    Map<String, Double> rolloverStoryPoints,
    Map<String, Double> resolvedRollover,
    int sprintIssueCount,
    int selectedIssueCount,
    List<String> selectedIssueKeys,
    List<String> plannedIssueKeys,
    List<String> committedIssueKeys,
    List<RolloverIssueDto> rolloverIssues,
    List<DomainPlanningMetricsDto> domainMetrics
) {
}
