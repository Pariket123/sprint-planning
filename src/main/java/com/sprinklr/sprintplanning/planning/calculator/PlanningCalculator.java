package com.sprinklr.sprintplanning.planning.calculator;

import com.sprinklr.sprintplanning.common.enums.Domain;
import com.sprinklr.sprintplanning.common.model.DomainAllocation;
import com.sprinklr.sprintplanning.common.model.IssueView;
import com.sprinklr.sprintplanning.common.util.IssueAllocationHelper;
import com.sprinklr.sprintplanning.planning.config.PlanningProperties;
import com.sprinklr.sprintplanning.planning.dto.CapacityRiskStatus;
import com.sprinklr.sprintplanning.planning.dto.DomainPlanningMetricsDto;
import com.sprinklr.sprintplanning.planning.dto.RiskLevel;
import com.sprinklr.sprintplanning.release.dto.ReleaseCapacitySummaryDto;
import com.sprinklr.sprintplanning.planning.dto.PlanningSummaryDto;
import com.sprinklr.sprintplanning.planning.dto.PlanningValidationResultDto;
import com.sprinklr.sprintplanning.planning.dto.PlanningWarningCode;
import com.sprinklr.sprintplanning.planning.dto.PlanningWarningDto;
import com.sprinklr.sprintplanning.planning.model.PersonCapacity;
import com.sprinklr.sprintplanning.planning.model.LeaveEntry;
import com.sprinklr.sprintplanning.planning.model.LeaveType;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class PlanningCalculator {

  private static final Set<Domain> DEFAULT_DOMAINS = EnumSet.of(Domain.DEV, Domain.QA, Domain.DESIGN);

  private final PlanningProperties properties;

  public PlanningCalculator(PlanningProperties properties) {
    this.properties = properties;
  }

  public Map<Domain, Double> computeRolloverFromIssues(List<IssueView> previousSprintIssues) {
    Set<Domain> domains = discoverDomainsFromIssues(previousSprintIssues);
    Map<Domain, Double> rollover = initDomainMap(domains);
    for (IssueView issue : nullSafeIssues(previousSprintIssues)) {
      for (DomainAllocation allocation : IssueAllocationHelper.effectiveAllocations(issue)) {
        if (allocation.completed()) {
          continue;
        }
        if (!isPlanningDomain(allocation.domain())) {
          continue;
        }
        rollover.merge(allocation.domain(), allocation.storyPoints(), Double::sum);
      }
    }
    return rollover;
  }

  public Map<Domain, Double> resolveRollover(
      Map<Domain, Double> computedRollover,
      Map<String, Double> manualOverrides) {
    Set<Domain> domains = discoverDomainsFromRollover(computedRollover, manualOverrides);
    Map<Domain, Double> resolved = initDomainMap(domains);
    if (computedRollover != null) {
      computedRollover.forEach((domain, value) -> {
        if (isPlanningDomain(domain) && value != null) {
          resolved.put(domain, value);
        }
      });
    }
    if (manualOverrides == null) {
      return resolved;
    }
    for (Domain domain : domains) {
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

    Set<Domain> planningDomains = discoverPlanningDomains(input);
    Map<Domain, Double> availableCapacity = calculateAvailableCapacity(
        input.capacity(), input.leaves(), sprintStart, sprintEnd, workingDays, planningDomains);
    Map<Domain, Double> rollover = resolveRollover(input.computedRollover(), input.manualRolloverOverrides());
    Map<Domain, SelectionMetrics> selection = calculateIssueMetrics(input.selectedIssues(), planningDomains);
    Map<Domain, SelectionMetrics> committed = calculateIssueMetrics(input.committedIssues(), planningDomains);

    List<DomainPlanningMetricsDto> domainMetrics = new ArrayList<>();
    double totalAvailable = 0;
    double totalRollover = 0;
    double totalSelected = 0;
    int totalIssueCount = 0;

    for (Domain domain : sortedDomains(planningDomains)) {
      double available = availableCapacity.getOrDefault(domain, 0.0);
      double domainRollover = rollover.getOrDefault(domain, 0.0);
      SelectionMetrics selectedMetrics = selection.getOrDefault(domain, SelectionMetrics.EMPTY);
      SelectionMetrics committedMetrics = committed.getOrDefault(domain, SelectionMetrics.EMPTY);
      double suggestedCommitment = Math.max(0, available - domainRollover);
      double committedStoryPoints = committedMetrics.storyPoints();
      double utilizationPercent = calculateUtilizationPercent(committedStoryPoints, available);

      domainMetrics.add(new DomainPlanningMetricsDto(
          domain,
          round(available),
          round(domainRollover),
          round(selectedMetrics.storyPoints()),
          selectedMetrics.issueCount(),
          round(suggestedCommitment),
          round(committedStoryPoints),
          round(utilizationPercent),
          determineCapacityRisk(committedStoryPoints, available)));

      totalAvailable += available;
      totalRollover += domainRollover;
      totalSelected += selectedMetrics.storyPoints();
      totalIssueCount += selectedMetrics.issueCount();
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

  public ReleaseCapacitySummaryDto calculateReleaseSummary(
      String releaseId,
      Integer durationDays,
      LocalDate releaseStart,
      List<PersonCapacity> capacity,
      double leavePercent,
      List<IssueView> issues) {
    int duration = durationDays != null ? Math.max(0, durationDays) : 0;
    LocalDate releaseEnd = resolveReleaseEnd(releaseStart, duration);

    int workingDays;
    if (releaseStart != null && releaseEnd != null) {
      workingDays = countBusinessDays(releaseStart, releaseEnd);
    } else {
      workingDays = duration;
    }

    Set<Domain> planningDomains = new LinkedHashSet<>();
    addDomainsFromCapacity(planningDomains, capacity);
    addDomainsFromIssues(planningDomains, issues);
    if (planningDomains.isEmpty()) {
      planningDomains.addAll(DEFAULT_DOMAINS);
    }

    Map<Domain, Double> availableCapacity = calculateAvailableCapacity(
        capacity, List.of(), releaseStart, releaseEnd, workingDays, planningDomains);
    double leaveFactor = 1.0 - (Math.min(100.0, Math.max(0.0, leavePercent)) / 100.0);
    for (Domain domain : planningDomains) {
      availableCapacity.put(domain, availableCapacity.getOrDefault(domain, 0.0) * leaveFactor);
    }

    Map<Domain, SelectionMetrics> issueMetrics = calculateIssueMetrics(issues, planningDomains);

    List<DomainPlanningMetricsDto> domainMetrics = new ArrayList<>();
    double totalAvailable = 0;
    double totalCommitted = 0;
    int totalIssueCount = 0;

    for (Domain domain : sortedDomains(planningDomains)) {
      double available = availableCapacity.getOrDefault(domain, 0.0);
      SelectionMetrics metrics = issueMetrics.getOrDefault(domain, SelectionMetrics.EMPTY);
      double committedStoryPoints = metrics.storyPoints();
      double utilizationPercent = calculateUtilizationPercent(committedStoryPoints, available);

      domainMetrics.add(new DomainPlanningMetricsDto(
          domain,
          round(available),
          0.0,
          round(committedStoryPoints),
          metrics.issueCount(),
          round(available),
          round(committedStoryPoints),
          round(utilizationPercent),
          determineCapacityRisk(committedStoryPoints, available)));

      totalAvailable += available;
      totalCommitted += committedStoryPoints;
      totalIssueCount += metrics.issueCount();
    }

    RiskLevel riskLevel = determineRiskLevel(totalCommitted, totalAvailable);

    return new ReleaseCapacitySummaryDto(
        releaseId,
        duration > 0 ? duration : null,
        round(totalAvailable),
        round(totalCommitted),
        totalIssueCount,
        domainMetrics,
        riskLevel);
  }

  LocalDate resolveReleaseEnd(LocalDate start, int durationDays) {
    if (start == null || durationDays <= 0) {
      return null;
    }
    int counted = 0;
    LocalDate date = start;
    while (true) {
      if (isBusinessDay(date)) {
        counted++;
        if (counted >= durationDays) {
          return date;
        }
      }
      date = date.plusDays(1);
    }
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
      if (metrics.capacityRisk() == CapacityRiskStatus.OVER_CAPACITY) {
        warnings.add(new PlanningWarningDto(
            PlanningWarningCode.OVER_CAPACITY,
            "Committed story points exceed available capacity for " + metrics.domain(),
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

  Set<Domain> discoverPlanningDomains(PlanningCalculationInput input) {
    Set<Domain> domains = new LinkedHashSet<>();
    addDomainsFromCapacity(domains, input.capacity());
    addDomainsFromLeaves(domains, input.leaves());
    addDomainsFromIssues(domains, input.selectedIssues());
    addDomainsFromIssues(domains, input.committedIssues());
    addDomainsFromRollover(domains, input.computedRollover(), input.manualRolloverOverrides());
    if (domains.isEmpty()) {
      domains.addAll(DEFAULT_DOMAINS);
    }
    return domains;
  }

  private Map<Domain, Double> calculateAvailableCapacity(
      List<PersonCapacity> capacity,
      List<LeaveEntry> leaves,
      LocalDate sprintStart,
      LocalDate sprintEnd,
      int workingDays,
      Set<Domain> planningDomains) {
    Map<Domain, Double> available = initDomainMap(planningDomains);

    if (capacity != null) {
      for (PersonCapacity entry : capacity) {
        if (entry == null || !isPlanningDomain(entry.getDomain())) {
          continue;
        }
        if (!planningDomains.contains(entry.getDomain())) {
          planningDomains.add(entry.getDomain());
          available.put(entry.getDomain(), 0.0);
        }
        double memberDays = (entry.getBandwidthPercent() / 100.0) * workingDays;
        available.merge(entry.getDomain(), memberDays, Double::sum);
      }
    }

    if (leaves != null) {
      for (LeaveEntry leave : leaves) {
        if (leave == null || leave.getType() != LeaveType.LEAVE) {
          continue;
        }
        int leaveDays = countOverlappingBusinessDays(leave.getStartDate(), leave.getEndDate(), sprintStart, sprintEnd);
        if (leaveDays <= 0) {
          continue;
        }
        double bandwidthPercent = resolveLeaveBandwidth(leave, capacity);
        double deduction = leaveDays * (bandwidthPercent / 100.0);
        if (leave.getDomain() == null) {
          for (Domain domain : planningDomains) {
            available.put(domain, available.get(domain) - deduction);
          }
        } else if (isPlanningDomain(leave.getDomain())) {
          available.merge(leave.getDomain(), -deduction, Double::sum);
        }
      }
    }

    for (Domain domain : planningDomains) {
      available.put(domain, Math.max(0, available.getOrDefault(domain, 0.0)));
    }
    return available;
  }

  private double resolveLeaveBandwidth(LeaveEntry leave, List<PersonCapacity> capacity) {
    if (leave.getPersonName() != null && !leave.getPersonName().isBlank() && capacity != null) {
      for (PersonCapacity person : capacity) {
        if (person != null
            && leave.getPersonName().equalsIgnoreCase(person.getPersonName())) {
          return person.getBandwidthPercent();
        }
      }
    }
    return 100.0;
  }

  private Map<Domain, SelectionMetrics> calculateIssueMetrics(List<IssueView> issues, Set<Domain> planningDomains) {
    Map<Domain, SelectionMetrics> metrics = new EnumMap<>(Domain.class);
    for (Domain domain : planningDomains) {
      metrics.put(domain, new SelectionMetrics());
    }
    for (IssueView issue : nullSafeIssues(issues)) {
      for (DomainAllocation allocation : IssueAllocationHelper.effectiveAllocations(issue)) {
        if (!isPlanningDomain(allocation.domain())) {
          continue;
        }
        metrics.computeIfAbsent(allocation.domain(), ignored -> new SelectionMetrics())
            .add(allocation.storyPoints());
      }
    }
    return metrics;
  }

  private Set<Domain> discoverDomainsFromIssues(List<IssueView> issues) {
    Set<Domain> domains = new LinkedHashSet<>();
    addDomainsFromIssues(domains, issues);
    if (domains.isEmpty()) {
      domains.addAll(DEFAULT_DOMAINS);
    }
    return domains;
  }

  private Set<Domain> discoverDomainsFromRollover(
      Map<Domain, Double> computedRollover,
      Map<String, Double> manualOverrides) {
    Set<Domain> domains = new LinkedHashSet<>();
    addDomainsFromRollover(domains, computedRollover, manualOverrides);
    if (domains.isEmpty()) {
      domains.addAll(DEFAULT_DOMAINS);
    }
    return domains;
  }

  private void addDomainsFromCapacity(Set<Domain> domains, List<PersonCapacity> capacity) {
    if (capacity == null) {
      return;
    }
    for (PersonCapacity entry : capacity) {
      if (entry != null && isPlanningDomain(entry.getDomain())) {
        domains.add(entry.getDomain());
      }
    }
  }

  private void addDomainsFromLeaves(Set<Domain> domains, List<LeaveEntry> leaves) {
    if (leaves == null) {
      return;
    }
    for (LeaveEntry leave : leaves) {
      if (leave != null && isPlanningDomain(leave.getDomain())) {
        domains.add(leave.getDomain());
      }
    }
  }

  private void addDomainsFromIssues(Set<Domain> domains, List<IssueView> issues) {
    for (IssueView issue : nullSafeIssues(issues)) {
      for (DomainAllocation allocation : IssueAllocationHelper.effectiveAllocations(issue)) {
        if (isPlanningDomain(allocation.domain())) {
          domains.add(allocation.domain());
        }
      }
    }
  }

  private void addDomainsFromRollover(
      Set<Domain> domains,
      Map<Domain, Double> computedRollover,
      Map<String, Double> manualOverrides) {
    if (computedRollover != null) {
      computedRollover.keySet().stream()
          .filter(this::isPlanningDomain)
          .forEach(domains::add);
    }
    if (manualOverrides != null) {
      for (String domainName : manualOverrides.keySet()) {
        parseDomain(domainName).ifPresent(domains::add);
      }
    }
  }

  private List<Domain> sortedDomains(Set<Domain> domains) {
    return domains.stream()
        .sorted(Comparator.comparing(Enum::name))
        .toList();
  }

  private java.util.Optional<Domain> parseDomain(String domainName) {
    if (domainName == null || domainName.isBlank()) {
      return java.util.Optional.empty();
    }
    try {
      Domain domain = Domain.valueOf(domainName.trim());
      return isPlanningDomain(domain) ? java.util.Optional.of(domain) : java.util.Optional.empty();
    } catch (IllegalArgumentException ex) {
      return java.util.Optional.empty();
    }
  }

  private boolean isPlanningDomain(Domain domain) {
    return domain != null && domain != Domain.UNKNOWN;
  }

  private double calculateUtilizationPercent(double committedStoryPoints, double availableCapacity) {
    if (availableCapacity <= 0) {
      return committedStoryPoints > 0 ? 100.0 : 0.0;
    }
    return (committedStoryPoints / availableCapacity) * 100.0;
  }

  private CapacityRiskStatus determineCapacityRisk(double committedStoryPoints, double availableCapacity) {
    if (availableCapacity <= 0) {
      return committedStoryPoints > 0 ? CapacityRiskStatus.OVER_CAPACITY : CapacityRiskStatus.OK;
    }
    double utilization = committedStoryPoints / availableCapacity;
    if (utilization > 1.0) {
      return CapacityRiskStatus.OVER_CAPACITY;
    }
    if (utilization >= properties.getHighUtilizationThreshold()) {
      return CapacityRiskStatus.NEAR_CAPACITY;
    }
    return CapacityRiskStatus.OK;
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

  private Map<Domain, Double> initDomainMap(Set<Domain> domains) {
    Map<Domain, Double> map = new EnumMap<>(Domain.class);
    for (Domain domain : domains) {
      map.put(domain, 0.0);
    }
    return map;
  }

  private List<IssueView> nullSafeIssues(List<IssueView> issues) {
    return issues != null ? issues : List.of();
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
