package com.sprinklr.sprintplanning.planning.service;

import com.sprinklr.sprintplanning.common.enums.Domain;
import com.sprinklr.sprintplanning.common.model.IssueView;
import com.sprinklr.sprintplanning.common.model.SprintView;
import com.sprinklr.sprintplanning.planning.calculator.PlanningCalculationInput;
import com.sprinklr.sprintplanning.planning.calculator.PlanningCalculator;
import com.sprinklr.sprintplanning.planning.dto.PlanningSummaryDto;
import com.sprinklr.sprintplanning.planning.dto.PlanningViewDto;
import com.sprinklr.sprintplanning.planning.mapper.PlanningMapper;
import com.sprinklr.sprintplanning.planning.model.RolloverIssue;
import com.sprinklr.sprintplanning.planning.model.SprintPlanningDocument;
import com.sprinklr.sprintplanning.planning.repository.SprintPlanningRepository;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class PlanningViewAssembler {

  private final PlanningCalculator planningCalculator;
  private final PlanningMapper planningMapper;
  private final SprintPlanningRepository sprintPlanningRepository;
  private final SprintPlanningDocumentAccessor planningDocumentAccessor;

  public PlanningViewAssembler(
      PlanningCalculator planningCalculator,
      PlanningMapper planningMapper,
      SprintPlanningRepository sprintPlanningRepository,
      SprintPlanningDocumentAccessor planningDocumentAccessor) {
    this.planningCalculator = planningCalculator;
    this.planningMapper = planningMapper;
    this.sprintPlanningRepository = sprintPlanningRepository;
    this.planningDocumentAccessor = planningDocumentAccessor;
  }

  public PlanningSummaryDto buildSummary(
      Long jiraSprintId,
      SprintView sprint,
      SprintPlanningDocument planning,
      Map<Domain, Double> computedRollover,
      List<IssueView> selectedIssues,
      List<IssueView> committedIssues) {
    PlanningCalculationInput input = new PlanningCalculationInput(
        jiraSprintId,
        sprint.startDate(),
        sprint.endDate(),
        planning.getCapacity(),
        planning.getLeaves(),
        planning.getRolloverStoryPoints(),
        computedRollover,
        selectedIssues,
        committedIssues,
        planning.getCapacityAllocation());
    return planningCalculator.calculateSummary(input);
  }

  public PlanningViewDto assemblePlanningView(
      Long jiraSprintId,
      SprintView sprint,
      SprintPlanningDocument planning,
      List<IssueView> sprintIssues,
      Map<Domain, Double> resolvedRollover,
      List<IssueView> selectedIssues,
      PlanningSummaryDto summary,
      List<String> committedKeys,
      String podId) {
    return new PlanningViewDto(
        jiraSprintId,
        sprint,
        planning.getCapacity(),
        planning.getLeaves(),
        planning.getOverrides(),
        planning.getCapacityAllocation(),
        planning.getRolloverStoryPoints(),
        planningMapper.toResolvedRolloverMap(resolvedRollover),
        sprintIssues.size(),
        summary.totalSelectedIssueCount(),
        summary.totalSelectedStoryPoints(),
        summary.totalCommittedIssueCount(),
        summary.totalCommittedStoryPoints(),
        summary.totalRoadmapCapacity(),
        summary.riskLevel(),
        selectedIssues.stream().map(IssueView::key).toList(),
        nullSafeCopy(planning.getPlannedIssueKeys()),
        committedKeys,
        countRolloverRecordsInvolvingSprint(podId, jiraSprintId, planning),
        summary.domainMetrics(),
        summary.capacityAllocationTable());
  }

  public Map<Domain, Double> resolveRolloverFromPreviousIssues(
      SprintPlanningDocument planning,
      List<IssueView> previousSprintIssues) {
    Map<Domain, Double> computed = planningCalculator.computeRolloverFromIssues(previousSprintIssues);
    return planningCalculator.resolveRollover(computed, planning.getRolloverStoryPoints());
  }

  public SprintView findPreviousSprint(List<SprintView> closedSprints, SprintView currentSprint) {
    var currentStart = currentSprint.startDate();

    return closedSprints.stream()
        .filter(sprint -> !sprint.id().equals(currentSprint.id()))
        .filter(sprint -> sprint.endDate() != null
            && (currentStart == null || !sprint.endDate().isAfter(currentStart)))
        .max(Comparator.comparing(SprintView::endDate, Comparator.nullsLast(Comparator.naturalOrder())))
        .orElse(null);
  }

  private int countRolloverRecordsInvolvingSprint(
      String podId, Long jiraSprintId, SprintPlanningDocument planning) {
    Map<String, Boolean> merged = new LinkedHashMap<>();
    for (RolloverIssue issue : planningDocumentAccessor.rolloverIssues(planning)) {
      if (jiraSprintId.equals(issue.getFromSprintId())) {
        merged.put(rolloverRecordKey(issue), Boolean.TRUE);
      }
    }
    for (SprintPlanningDocument document : sprintPlanningRepository.findIncomingRollovers(podId, jiraSprintId)) {
      for (RolloverIssue issue : planningDocumentAccessor.rolloverIssues(document)) {
        if (jiraSprintId.equals(issue.getToSprintId())) {
          merged.put(rolloverRecordKey(issue), Boolean.TRUE);
        }
      }
    }
    return merged.size();
  }

  private String rolloverRecordKey(RolloverIssue issue) {
    return issue.getIssueKey() + ":" + issue.getFromSprintId() + ":" + issue.getToSprintId();
  }

  private List<String> nullSafeCopy(List<String> values) {
    return values != null ? List.copyOf(values) : List.of();
  }
}
