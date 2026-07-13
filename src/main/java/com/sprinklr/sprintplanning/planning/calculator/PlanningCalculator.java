package com.sprinklr.sprintplanning.planning.calculator;

import com.sprinklr.sprintplanning.common.enums.Domain;
import com.sprinklr.sprintplanning.planning.dto.PlanningSummaryDto;
import com.sprinklr.sprintplanning.planning.dto.PlanningValidationResultDto;
import com.sprinklr.sprintplanning.release.dto.ReleaseCapacitySummaryDto;
import com.sprinklr.sprintplanning.common.model.IssueView;
import com.sprinklr.sprintplanning.planning.model.CapacityAllocationPercents;
import com.sprinklr.sprintplanning.planning.model.PersonCapacity;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Component
public class PlanningCalculator {

  private final CapacityCalculator capacityCalculator;
  private final RolloverCalculator rolloverCalculator;
  private final PlanningValidator planningValidator;
  private final PlanningSummaryAssembler summaryAssembler;

  public PlanningCalculator(
      CapacityCalculator capacityCalculator,
      RolloverCalculator rolloverCalculator,
      PlanningValidator planningValidator,
      PlanningSummaryAssembler summaryAssembler) {
    this.capacityCalculator = capacityCalculator;
    this.rolloverCalculator = rolloverCalculator;
    this.planningValidator = planningValidator;
    this.summaryAssembler = summaryAssembler;
  }

  public Map<Domain, Double> computeRolloverFromIssues(List<IssueView> previousSprintIssues) {
    return rolloverCalculator.computeRolloverFromIssues(previousSprintIssues);
  }

  public Map<Domain, Double> resolveRollover(
      Map<Domain, Double> computedRollover,
      Map<String, Double> manualOverrides) {
    return rolloverCalculator.resolveRollover(computedRollover, manualOverrides);
  }

  public PlanningSummaryDto calculateSummary(PlanningCalculationInput input) {
    return summaryAssembler.calculateSummary(input);
  }

  public ReleaseCapacitySummaryDto calculateReleaseSummary(
      String releaseId,
      Integer durationDays,
      LocalDate releaseStart,
      List<PersonCapacity> capacity,
      double leavePercent,
      List<CapacityAllocationPercents> capacityAllocation,
      List<IssueView> issues) {
    return summaryAssembler.calculateReleaseSummary(
        releaseId, durationDays, releaseStart, capacity, leavePercent, capacityAllocation, issues);
  }

  public PlanningValidationResultDto validate(PlanningSummaryDto summary) {
    return planningValidator.validate(summary);
  }

  int countBusinessDays(LocalDate start, LocalDate end) {
    return capacityCalculator.countBusinessDays(start, end);
  }

  LocalDate resolveReleaseEnd(LocalDate start, int durationDays) {
    return capacityCalculator.resolveReleaseEnd(start, durationDays);
  }
}
