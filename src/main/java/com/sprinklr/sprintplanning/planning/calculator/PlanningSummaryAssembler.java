package com.sprinklr.sprintplanning.planning.calculator;

import com.sprinklr.sprintplanning.common.enums.Domain;
import com.sprinklr.sprintplanning.common.model.IssueView;
import com.sprinklr.sprintplanning.planning.config.PlanningProperties;
import com.sprinklr.sprintplanning.planning.dto.CapacityAllocationTableDto;
import com.sprinklr.sprintplanning.planning.dto.DomainPlanningMetricsDto;
import com.sprinklr.sprintplanning.planning.dto.PlanningSummaryDto;
import com.sprinklr.sprintplanning.planning.dto.RiskLevel;
import com.sprinklr.sprintplanning.planning.model.CapacityAllocationPercents;
import com.sprinklr.sprintplanning.planning.model.PersonCapacity;
import com.sprinklr.sprintplanning.release.dto.ReleaseCapacitySummaryDto;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class PlanningSummaryAssembler {

  private final CapacityCalculator capacityCalculator;
  private final RolloverCalculator rolloverCalculator;
  private final CapacityAllocationCalculator capacityAllocationCalculator;
  private final PlanningMathSupport mathSupport;
  private final PlanningDomainSupport domainSupport;

  public PlanningSummaryAssembler(
      CapacityCalculator capacityCalculator,
      RolloverCalculator rolloverCalculator,
      CapacityAllocationCalculator capacityAllocationCalculator,
      PlanningProperties properties) {
    this.capacityCalculator = capacityCalculator;
    this.rolloverCalculator = rolloverCalculator;
    this.capacityAllocationCalculator = capacityAllocationCalculator;
    this.mathSupport = new PlanningMathSupport(properties);
    this.domainSupport = new PlanningDomainSupport();
  }

  public PlanningSummaryDto calculateSummary(PlanningCalculationInput input) {
    LocalDate sprintStart = capacityCalculator.toLocalDate(input.sprintStart());
    LocalDate sprintEnd = capacityCalculator.toLocalDate(input.sprintEnd());

    int sprintBusinessDays = capacityCalculator.countBusinessDays(sprintStart, sprintEnd);
    int holidayDays = capacityCalculator.countHolidayDays(input.leaves(), sprintStart, sprintEnd);
    int workingDays = Math.max(0, sprintBusinessDays - holidayDays);

    Set<Domain> planningDomains = capacityCalculator.discoverPlanningDomains(input);
    Map<Domain, Double> availableCapacity = capacityCalculator.calculateAvailableCapacity(
        input.capacity(), input.leaves(), sprintStart, sprintEnd, workingDays, planningDomains);
    Map<Domain, Double> rollover = rolloverCalculator.resolveRollover(
        input.computedRollover(), input.manualRolloverOverrides());
    Map<Domain, SelectionMetrics> selection = capacityCalculator.calculateIssueMetrics(
        input.selectedIssues(), planningDomains);
    Map<Domain, SelectionMetrics> committed = capacityCalculator.calculateIssueMetrics(
        input.committedIssues(), planningDomains);
    CapacityAllocationTableDto allocationTable = capacityAllocationCalculator.buildTable(
        availableCapacity, input.capacityAllocation());

    DomainMetricsTotals metricsTotals = buildDomainMetrics(
        availableCapacity, rollover, selection, committed, allocationTable, true);

    double totalSelected = mathSupport.sumUniqueIssueStoryPoints(input.selectedIssues(), domainSupport);
    int totalIssueCount = mathSupport.countUniqueIssues(input.selectedIssues(), domainSupport);
    double totalCommittedStoryPoints = mathSupport.sumUniqueIssueStoryPoints(
        input.committedIssues(), domainSupport);
    int totalCommittedIssueCount = mathSupport.countUniqueIssues(input.committedIssues(), domainSupport);

    double totalPlannedRoadmap = capacityAllocationCalculator.plannedRoadmapStoryPoints(
        allocationTable, null);
    RiskLevel riskLevel = mathSupport.determineRiskLevel(totalCommittedStoryPoints, totalPlannedRoadmap);

    return new PlanningSummaryDto(
        input.jiraSprintId(),
        mathSupport.round(metricsTotals.totalAvailable()),
        mathSupport.round(metricsTotals.totalRollover()),
        mathSupport.round(totalSelected),
        totalIssueCount,
        mathSupport.round(totalCommittedStoryPoints),
        totalCommittedIssueCount,
        mathSupport.round(totalPlannedRoadmap),
        metricsTotals.domainMetrics(),
        riskLevel,
        allocationTable);
  }

  public ReleaseCapacitySummaryDto calculateReleaseSummary(
      String releaseId,
      Integer durationDays,
      LocalDate releaseStart,
      List<PersonCapacity> capacity,
      double leavePercent,
      List<CapacityAllocationPercents> capacityAllocation,
      List<IssueView> issues) {
    int duration = durationDays != null ? Math.max(0, durationDays) : 0;
    LocalDate releaseEnd = capacityCalculator.resolveReleaseEnd(releaseStart, duration);

    int workingDays;
    if (releaseStart != null && releaseEnd != null) {
      workingDays = capacityCalculator.countBusinessDays(releaseStart, releaseEnd);
    } else {
      workingDays = duration;
    }

    Set<Domain> planningDomains = new LinkedHashSet<>();
    domainSupport.addDomainsFromCapacity(planningDomains, capacity);
    domainSupport.addDomainsFromIssues(planningDomains, issues);
    if (planningDomains.isEmpty()) {
      planningDomains.addAll(PlanningDomainSupport.DEFAULT_DOMAINS);
    }

    Map<Domain, Double> availableCapacity = capacityCalculator.calculateAvailableCapacity(
        capacity, List.of(), releaseStart, releaseEnd, workingDays, planningDomains);
    double leaveFactor = 1.0 - (Math.min(100.0, Math.max(0.0, leavePercent)) / 100.0);
    for (Domain domain : planningDomains) {
      availableCapacity.put(domain, availableCapacity.getOrDefault(domain, 0.0) * leaveFactor);
    }

    Map<Domain, SelectionMetrics> issueMetrics = capacityCalculator.calculateIssueMetrics(issues, planningDomains);
    CapacityAllocationTableDto allocationTable = capacityAllocationCalculator.buildTable(
        availableCapacity, capacityAllocation);

    DomainMetricsTotals metricsTotals = buildDomainMetrics(
        availableCapacity,
        Map.of(),
        issueMetrics,
        issueMetrics,
        allocationTable,
        false);

    double totalCommittedStoryPoints = mathSupport.sumUniqueIssueStoryPoints(issues, domainSupport);
    int totalIssueCount = mathSupport.countUniqueIssues(issues, domainSupport);

    double totalPlannedRoadmap = capacityAllocationCalculator.plannedRoadmapStoryPoints(
        allocationTable, null);
    RiskLevel riskLevel = mathSupport.determineRiskLevel(totalCommittedStoryPoints, totalPlannedRoadmap);

    return new ReleaseCapacitySummaryDto(
        releaseId,
        duration > 0 ? duration : null,
        mathSupport.round(metricsTotals.totalAvailable()),
        mathSupport.round(totalCommittedStoryPoints),
        totalIssueCount,
        metricsTotals.domainMetrics(),
        riskLevel,
        allocationTable);
  }

  private DomainMetricsTotals buildDomainMetrics(
      Map<Domain, Double> availableCapacity,
      Map<Domain, Double> rollover,
      Map<Domain, SelectionMetrics> selection,
      Map<Domain, SelectionMetrics> committed,
      CapacityAllocationTableDto allocationTable,
      boolean includeRollover) {
    List<DomainPlanningMetricsDto> domainMetrics = new ArrayList<>();
    double totalAvailable = 0;
    double totalRollover = 0;

    for (Domain domain : CapacityAllocationCalculator.ENGINEERING_DOMAINS) {
      double available = availableCapacity.getOrDefault(domain, 0.0);
      double domainRollover = includeRollover ? rollover.getOrDefault(domain, 0.0) : 0.0;
      SelectionMetrics selectedMetrics = selection.getOrDefault(domain, SelectionMetrics.EMPTY);
      SelectionMetrics committedMetrics = committed.getOrDefault(domain, SelectionMetrics.EMPTY);
      double committedStoryPoints = committedMetrics.storyPoints();
      double plannedRoadmapCapacity =
          capacityAllocationCalculator.plannedRoadmapStoryPoints(allocationTable, domain);
      double utilizationPercent = mathSupport.calculateUtilizationPercent(
          committedStoryPoints, plannedRoadmapCapacity);

      domainMetrics.add(new DomainPlanningMetricsDto(
          domain,
          mathSupport.round(available),
          mathSupport.round(domainRollover),
          mathSupport.round(selectedMetrics.storyPoints()),
          selectedMetrics.issueCount(),
          mathSupport.round(plannedRoadmapCapacity),
          mathSupport.round(committedStoryPoints),
          mathSupport.round(utilizationPercent),
          mathSupport.determineCapacityRisk(committedStoryPoints, plannedRoadmapCapacity)));

      totalAvailable += available;
      if (includeRollover) {
        totalRollover += domainRollover;
      }
    }

    return new DomainMetricsTotals(domainMetrics, totalAvailable, totalRollover);
  }

  private record DomainMetricsTotals(
      List<DomainPlanningMetricsDto> domainMetrics,
      double totalAvailable,
      double totalRollover) {
  }
}
