package com.sprinklr.sprintplanning.planning.calculator;

import com.sprinklr.sprintplanning.common.enums.Domain;
import com.sprinklr.sprintplanning.common.enums.StatusCategory;
import com.sprinklr.sprintplanning.common.model.IssueView;
import com.sprinklr.sprintplanning.planning.config.PlanningProperties;
import com.sprinklr.sprintplanning.planning.dto.DomainPlanningMetricsDto;
import com.sprinklr.sprintplanning.planning.dto.PlanningSummaryDto;
import com.sprinklr.sprintplanning.planning.dto.PlanningValidationResultDto;
import com.sprinklr.sprintplanning.planning.dto.PlanningWarningCode;
import com.sprinklr.sprintplanning.planning.dto.PlanningWarningDto;
import com.sprinklr.sprintplanning.planning.dto.RiskLevel;
import com.sprinklr.sprintplanning.planning.model.DomainCapacity;
import com.sprinklr.sprintplanning.planning.model.LeaveEntry;
import com.sprinklr.sprintplanning.planning.model.LeaveType;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class PlanningCalculator {

  private static final Set<Domain> PLANNING_DOMAINS = EnumSet.of(Domain.DEV, Domain.QA, Domain.DESIGN);

  private final PlanningProperties properties;

  public PlanningCalculator(PlanningProperties properties) {
    this.properties = properties;
  }

  public Map<Domain, Double> computeRolloverFromIssues(List<IssueView> previousSprintIssues) {
    Map<Domain, Double> rollover = initDomainMap();
    for (IssueView issue : previousSprintIssues) {
      if (issue.statusCategory() == StatusCategory.DONE) {
        continue;
      }
      if (!PLANNING_DOMAINS.contains(issue.domain())) {
        continue;
      }
      rollover.merge(issue.domain(), storyPointsOrZero(issue.storyPoints()), Double::sum);
    }
    return rollover;
  }

  public Map<Domain, Double> resolveRollover(
      Map<Domain, Double> computedRollover,
      Map<String, Double> manualOverrides) {
    Map<Domain, Double> resolved = initDomainMap();
    if (computedRollover != null) {
      computedRollover.forEach((domain, value) -> {
        if (PLANNING_DOMAINS.contains(domain) && value != null) {
          resolved.put(domain, value);
        }
      });
    }
    if (manualOverrides == null) {
      return resolved;
    }
    for (Domain domain : PLANNING_DOMAINS) {
      Double override = manualOverrides.get(domain.name());
      if (override != null) {
        resolved.put(domain, override);
      }
    }
    return resolved;
  }

  public PlanningSummaryDto calculateSummary(PlanningCalculationInput input) {
    LocalDate sprintStart = toLocalDate(input.sprintStart());
    LocalDate sprintEnd = toLocalDate(input.sprintEnd());

    int sprintBusinessDays = countBusinessDays(sprintStart, sprintEnd);
    int holidayDays = countHolidayDays(input.leaves(), sprintStart, sprintEnd);
    int workingDays = Math.max(0, sprintBusinessDays - holidayDays);

    Map<Domain, Double> availableCapacity = calculateAvailableCapacity(
        input.capacity(), input.leaves(), sprintStart, sprintEnd, workingDays);
    Map<Domain, Double> rollover = resolveRollover(input.computedRollover(), input.manualRolloverOverrides());
    Map<Domain, SelectionMetrics> selection = calculateSelectionMetrics(input.selectedIssues());

    List<DomainPlanningMetricsDto> domainMetrics = new ArrayList<>();
    double totalAvailable = 0;
    double totalRollover = 0;
    double totalSelected = 0;
    int totalIssueCount = 0;

    for (Domain domain : PLANNING_DOMAINS) {
      double available = availableCapacity.getOrDefault(domain, 0.0);
      double domainRollover = rollover.getOrDefault(domain, 0.0);
      SelectionMetrics metrics = selection.getOrDefault(domain, SelectionMetrics.EMPTY);
      double suggestedCommitment = Math.max(0, available - domainRollover);

      domainMetrics.add(new DomainPlanningMetricsDto(
          domain,
          round(available),
          round(domainRollover),
          round(metrics.storyPoints()),
          metrics.issueCount(),
          round(suggestedCommitment)));

      totalAvailable += available;
      totalRollover += domainRollover;
      totalSelected += metrics.storyPoints();
      totalIssueCount += metrics.issueCount();
    }

    RiskLevel riskLevel = determineRiskLevel(totalSelected, totalAvailable);

    return new PlanningSummaryDto(
        input.jiraSprintId(),
        round(totalAvailable),
        round(totalRollover),
        round(totalSelected),
        totalIssueCount,
        domainMetrics,
        riskLevel);
  }

  public PlanningValidationResultDto validate(PlanningSummaryDto summary) {
    List<PlanningWarningDto> warnings = new ArrayList<>();

    for (DomainPlanningMetricsDto metrics : summary.domainMetrics()) {
      if (metrics.selectedStoryPoints() > metrics.availableCapacity()) {
        warnings.add(new PlanningWarningDto(
            PlanningWarningCode.OVER_CAPACITY,
            "Selected story points exceed available capacity for " + metrics.domain(),
            metrics.domain()));
      }
    }

    if (summary.totalSelectedStoryPoints() > 0) {
      for (DomainPlanningMetricsDto metrics : summary.domainMetrics()) {
        double share = metrics.selectedStoryPoints() / summary.totalSelectedStoryPoints();
        if (share > properties.getDomainImbalanceThreshold()) {
          warnings.add(new PlanningWarningDto(
              PlanningWarningCode.DOMAIN_IMBALANCE,
              metrics.domain() + " accounts for more than "
                  + (int) (properties.getDomainImbalanceThreshold() * 100)
                  + "% of selected story points",
              metrics.domain()));
        }
      }
    }

    if (summary.totalAvailableCapacity() > 0) {
      double utilization = summary.totalSelectedStoryPoints() / summary.totalAvailableCapacity();
      if (utilization > properties.getHighUtilizationThreshold()) {
        warnings.add(new PlanningWarningDto(
            PlanningWarningCode.HIGH_UTILIZATION,
            "Total selected story points exceed "
                + (int) (properties.getHighUtilizationThreshold() * 100)
                + "% of available capacity",
            null));
      }
    }

    RiskLevel riskLevel = summary.riskLevel();
    if (warnings.stream().anyMatch(w -> w.code() == PlanningWarningCode.OVER_CAPACITY)) {
      riskLevel = RiskLevel.HIGH;
    }

    return new PlanningValidationResultDto(warnings, riskLevel);
  }

  private Map<Domain, Double> calculateAvailableCapacity(
      List<DomainCapacity> capacity,
      List<LeaveEntry> leaves,
      LocalDate sprintStart,
      LocalDate sprintEnd,
      int workingDays) {
    Map<Domain, Double> available = initDomainMap();

    if (capacity != null) {
      for (DomainCapacity entry : capacity) {
        if (entry == null || entry.getDomain() == null || !PLANNING_DOMAINS.contains(entry.getDomain())) {
          continue;
        }
        double memberDays = entry.getHeadcount()
            * (entry.getBandwidthPercent() / 100.0)
            * workingDays;
        if (entry.getManualAdjustment() != null) {
          memberDays += entry.getManualAdjustment();
        }
        available.put(entry.getDomain(), memberDays);
      }
    }

    if (leaves != null) {
      for (LeaveEntry leave : leaves) {
        if (leave == null || leave.getType() != LeaveType.LEAVE) {
          continue;
        }
        int leaveDays = countOverlappingBusinessDays(leave.getStartDate(), leave.getEndDate(), sprintStart, sprintEnd);
        if (leave.getDomain() == null) {
          for (Domain domain : PLANNING_DOMAINS) {
            available.put(domain, available.get(domain) - leaveDays);
          }
        } else if (PLANNING_DOMAINS.contains(leave.getDomain())) {
          available.put(leave.getDomain(), available.get(leave.getDomain()) - leaveDays);
        }
      }
    }

    for (Domain domain : PLANNING_DOMAINS) {
      available.put(domain, Math.max(0, available.get(domain)));
    }
    return available;
  }

  private Map<Domain, SelectionMetrics> calculateSelectionMetrics(List<IssueView> issues) {
    Map<Domain, SelectionMetrics> metrics = new EnumMap<>(Domain.class);
    for (Domain domain : PLANNING_DOMAINS) {
      metrics.put(domain, new SelectionMetrics());
    }
    if (issues == null) {
      return metrics;
    }
    for (IssueView issue : issues) {
      if (!PLANNING_DOMAINS.contains(issue.domain())) {
        continue;
      }
      metrics.get(issue.domain()).add(storyPointsOrZero(issue.storyPoints()));
    }
    return metrics;
  }

  private int countHolidayDays(List<LeaveEntry> leaves, LocalDate sprintStart, LocalDate sprintEnd) {
    if (leaves == null) {
      return 0;
    }
    int holidayDays = 0;
    for (LeaveEntry leave : leaves) {
      if (leave != null && leave.getType() == LeaveType.HOLIDAY) {
        holidayDays += countOverlappingBusinessDays(leave.getStartDate(), leave.getEndDate(), sprintStart, sprintEnd);
      }
    }
    return holidayDays;
  }

  int countBusinessDays(LocalDate start, LocalDate end) {
    if (start == null || end == null || end.isBefore(start)) {
      return 0;
    }
    int days = 0;
    for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
      if (isBusinessDay(date)) {
        days++;
      }
    }
    return days;
  }

  private int countOverlappingBusinessDays(
      LocalDate leaveStart, LocalDate leaveEnd, LocalDate sprintStart, LocalDate sprintEnd) {
    if (leaveStart == null || leaveEnd == null || sprintStart == null || sprintEnd == null) {
      return 0;
    }
    LocalDate start = leaveStart.isBefore(sprintStart) ? sprintStart : leaveStart;
    LocalDate end = leaveEnd.isAfter(sprintEnd) ? sprintEnd : leaveEnd;
    return countBusinessDays(start, end);
  }

  private boolean isBusinessDay(LocalDate date) {
    DayOfWeek day = date.getDayOfWeek();
    return day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY;
  }

  private LocalDate toLocalDate(Instant instant) {
    return instant != null ? instant.atZone(ZoneOffset.UTC).toLocalDate() : null;
  }

  private RiskLevel determineRiskLevel(double totalSelected, double totalAvailable) {
    if (totalAvailable <= 0) {
      return totalSelected > 0 ? RiskLevel.HIGH : RiskLevel.LOW;
    }
    double utilization = totalSelected / totalAvailable;
    if (utilization > properties.getHighUtilizationThreshold()) {
      return RiskLevel.HIGH;
    }
    if (utilization > properties.getMediumUtilizationThreshold()) {
      return RiskLevel.MEDIUM;
    }
    return RiskLevel.LOW;
  }

  private Map<Domain, Double> initDomainMap() {
    Map<Domain, Double> map = new EnumMap<>(Domain.class);
    for (Domain domain : PLANNING_DOMAINS) {
      map.put(domain, 0.0);
    }
    return map;
  }

  private double storyPointsOrZero(Double storyPoints) {
    return storyPoints != null ? storyPoints : 0.0;
  }

  private double round(double value) {
    return Math.round(value * 100.0) / 100.0;
  }

  private static class SelectionMetrics {
    private static final SelectionMetrics EMPTY = new SelectionMetrics();

    private double storyPoints;
    private int issueCount;

    void add(double points) {
      storyPoints += points;
      issueCount++;
    }

    double storyPoints() {
      return storyPoints;
    }

    int issueCount() {
      return issueCount;
    }
  }
}
