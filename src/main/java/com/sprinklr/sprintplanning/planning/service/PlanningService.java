package com.sprinklr.sprintplanning.planning.service;

import com.sprinklr.sprintplanning.common.enums.Domain;
import com.sprinklr.sprintplanning.common.model.IssueView;
import com.sprinklr.sprintplanning.planning.dto.BacklogPageDto;
import com.sprinklr.sprintplanning.planning.dto.IssueMoveRequest;
import com.sprinklr.sprintplanning.planning.dto.PlannedIssueViewDto;
import com.sprinklr.sprintplanning.planning.dto.PlannedScopeDto;
import com.sprinklr.sprintplanning.planning.dto.PlanningIssuesPageDto;
import com.sprinklr.sprintplanning.planning.dto.PlanningSummaryDto;
import com.sprinklr.sprintplanning.planning.dto.PlanningValidationResultDto;
import com.sprinklr.sprintplanning.planning.dto.PlanningViewDto;
import com.sprinklr.sprintplanning.planning.model.PersonCapacity;
import com.sprinklr.sprintplanning.planning.model.LeaveEntry;
import com.sprinklr.sprintplanning.planning.model.CapacityAllocationPercents;
import com.sprinklr.sprintplanning.planning.model.PlanningOverride;
import com.sprinklr.sprintplanning.planning.model.SprintPlanningDocument;

import java.util.List;
import java.util.Map;

public interface PlanningService {

  SprintPlanningDocument getOrCreatePlanning(String podId, Long jiraSprintId);

  SprintPlanningDocument updateCapacity(String podId, Long jiraSprintId, List<PersonCapacity> capacity);

  SprintPlanningDocument updateLeaves(String podId, Long jiraSprintId, List<LeaveEntry> leaves);

  SprintPlanningDocument updateOverrides(String podId, Long jiraSprintId, List<PlanningOverride> overrides);

  SprintPlanningDocument updateCapacityAllocation(
      String podId, Long jiraSprintId, List<CapacityAllocationPercents> capacityAllocation);

  SprintPlanningDocument updateRolloverOverrides(
      String podId, Long jiraSprintId, Map<String, Double> rolloverStoryPoints);

  PlanningSummaryDto calculateSummary(String podId, Long jiraSprintId);

  PlanningValidationResultDto validate(String podId, Long jiraSprintId);

  Map<Domain, Double> computeRollover(String podId, Long jiraSprintId);

  List<IssueView> resolveSelectedIssues(String podId, Long jiraSprintId);

  PlanningViewDto getPlanningView(String podId, Long jiraSprintId);

  PlanningIssuesPageDto getPlanningIssues(String podId, Long jiraSprintId, int startAt, int maxResults);

  PlannedScopeDto getPlannedScope(String podId, Long jiraSprintId);

  PlannedScopeDto updatePlannedScope(String podId, Long jiraSprintId, List<String> plannedIssueKeys);

  List<PlannedIssueViewDto> getPlannedIssues(String podId, Long jiraSprintId);

  BacklogPageDto getBacklog(String podId, int startAt, int maxResults);

  PlanningViewDto moveIssuesToSprint(String podId, Long jiraSprintId, IssueMoveRequest request);

  BacklogPageDto moveIssuesToBacklog(String podId, int startAt, int maxResults, List<String> issueKeys);
}
