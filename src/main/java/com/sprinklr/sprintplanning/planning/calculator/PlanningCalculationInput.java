package com.sprinklr.sprintplanning.planning.calculator;

import com.sprinklr.sprintplanning.common.model.IssueView;
import com.sprinklr.sprintplanning.planning.model.DomainCapacity;
import com.sprinklr.sprintplanning.planning.model.LeaveEntry;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record PlanningCalculationInput(
    Long jiraSprintId,
    Instant sprintStart,
    Instant sprintEnd,
    List<DomainCapacity> capacity,
    List<LeaveEntry> leaves,
    Map<String, Double> manualRolloverOverrides,
    Map<com.sprinklr.sprintplanning.common.enums.Domain, Double> computedRollover,
    List<IssueView> selectedIssues
) {
}
