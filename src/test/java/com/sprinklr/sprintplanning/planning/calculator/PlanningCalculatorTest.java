package com.sprinklr.sprintplanning.planning.calculator;

import com.sprinklr.sprintplanning.common.enums.Domain;
import com.sprinklr.sprintplanning.common.enums.StatusCategory;
import com.sprinklr.sprintplanning.common.model.IssueView;
import com.sprinklr.sprintplanning.planning.config.PlanningProperties;
import com.sprinklr.sprintplanning.planning.dto.PlanningWarningCode;
import com.sprinklr.sprintplanning.planning.dto.RiskLevel;
import com.sprinklr.sprintplanning.planning.model.DomainCapacity;
import com.sprinklr.sprintplanning.planning.model.LeaveEntry;
import com.sprinklr.sprintplanning.planning.model.LeaveType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PlanningCalculatorTest {

  private PlanningCalculator calculator;

  @BeforeEach
  void setUp() {
    PlanningProperties properties = new PlanningProperties();
    properties.setDomainImbalanceThreshold(0.6);
    properties.setHighUtilizationThreshold(0.9);
    properties.setMediumUtilizationThreshold(0.75);
    calculator = new PlanningCalculator(properties);
  }

  @Test
  void calculatesAvailableCapacityWithLeavesAndHolidays() {
    List<DomainCapacity> capacity = List.of(devCapacity(2, 100.0));
    List<LeaveEntry> leaves = List.of(
        leave(LocalDate.of(2026, 6, 10), LocalDate.of(2026, 6, 10), LeaveType.HOLIDAY, null),
        leave(LocalDate.of(2026, 6, 11), LocalDate.of(2026, 6, 11), LeaveType.LEAVE, Domain.DEV));

  // Mon 8 - Fri 19 June 2026 = 10 business days; minus 1 holiday = 9 working days; minus 1 dev leave
    PlanningCalculationInput input = new PlanningCalculationInput(
        10L,
        Instant.parse("2026-06-08T00:00:00Z"),
        Instant.parse("2026-06-19T00:00:00Z"),
        capacity,
        leaves,
        Map.of(),
        Map.of(Domain.DEV, 2.0, Domain.QA, 0.0, Domain.DESIGN, 0.0),
        List.of(new IssueView("WFM-1", "Story", Domain.DEV, 5.0, "Story", "To Do", StatusCategory.TODO)));

    var summary = calculator.calculateSummary(input);

    assertThat(summary.domainMetrics()).filteredOn(m -> m.domain() == Domain.DEV).first()
        .satisfies(dev -> {
          assertThat(dev.availableCapacity()).isEqualTo(17.0);
          assertThat(dev.rollover()).isEqualTo(2.0);
          assertThat(dev.suggestedCommitment()).isEqualTo(15.0);
          assertThat(dev.selectedStoryPoints()).isEqualTo(5.0);
        });
    assertThat(summary.riskLevel()).isEqualTo(RiskLevel.LOW);
  }

  @Test
  void computesRolloverFromIncompletePreviousSprintIssues() {
    List<IssueView> previousIssues = List.of(
        new IssueView("WFM-1", "Done", Domain.DEV, 3.0, "Story", "Done", StatusCategory.DONE),
        new IssueView("WFM-2", "Open", Domain.QA, 4.0, "Bug", "To Do", StatusCategory.TODO));

    Map<Domain, Double> rollover = calculator.computeRolloverFromIssues(previousIssues);

    assertThat(rollover.get(Domain.DEV)).isEqualTo(0.0);
    assertThat(rollover.get(Domain.QA)).isEqualTo(4.0);
  }

  @Test
  void manualRolloverOverrideTakesPrecedence() {
    Map<Domain, Double> resolved = calculator.resolveRollover(
        Map.of(Domain.DEV, 5.0),
        Map.of("DEV", 8.0));

    assertThat(resolved.get(Domain.DEV)).isEqualTo(8.0);
  }

  @Test
  void validatesOverCapacityAndDomainImbalance() {
    List<DomainCapacity> capacity = List.of(
        devCapacity(1, 100.0),
        qaCapacity(1, 100.0));

    PlanningCalculationInput input = new PlanningCalculationInput(
        11L,
        Instant.parse("2026-06-08T00:00:00Z"),
        Instant.parse("2026-06-12T00:00:00Z"),
        capacity,
        List.of(),
        Map.of(),
        Map.of(Domain.DEV, 0.0, Domain.QA, 0.0, Domain.DESIGN, 0.0),
        List.of(
            new IssueView("WFM-1", "A", Domain.DEV, 8.0, "Story", "To Do", StatusCategory.TODO),
            new IssueView("WFM-2", "B", Domain.DEV, 7.0, "Story", "To Do", StatusCategory.TODO)));

    var summary = calculator.calculateSummary(input);
    var validation = calculator.validate(summary);

    assertThat(validation.warnings()).extracting("code")
        .contains(PlanningWarningCode.OVER_CAPACITY, PlanningWarningCode.DOMAIN_IMBALANCE);
    assertThat(validation.riskLevel()).isEqualTo(RiskLevel.HIGH);
  }

  @Test
  void countsBusinessDaysExcludingWeekends() {
    assertThat(calculator.countBusinessDays(LocalDate.of(2026, 6, 8), LocalDate.of(2026, 6, 14)))
        .isEqualTo(5);
  }

  private static DomainCapacity devCapacity(int headcount, double bandwidth) {
    DomainCapacity capacity = new DomainCapacity();
    capacity.setDomain(Domain.DEV);
    capacity.setHeadcount(headcount);
    capacity.setBandwidthPercent(bandwidth);
    return capacity;
  }

  private static DomainCapacity qaCapacity(int headcount, double bandwidth) {
    DomainCapacity capacity = new DomainCapacity();
    capacity.setDomain(Domain.QA);
    capacity.setHeadcount(headcount);
    capacity.setBandwidthPercent(bandwidth);
    return capacity;
  }

  private static LeaveEntry leave(LocalDate start, LocalDate end, LeaveType type, Domain domain) {
    LeaveEntry entry = new LeaveEntry();
    entry.setStartDate(start);
    entry.setEndDate(end);
    entry.setType(type);
    entry.setDomain(domain);
    return entry;
  }
}
