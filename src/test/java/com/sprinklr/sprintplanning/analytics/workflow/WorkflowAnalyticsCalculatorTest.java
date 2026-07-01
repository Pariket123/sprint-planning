package com.sprinklr.sprintplanning.analytics.workflow;

import com.sprinklr.sprintplanning.analytics.dto.DevSubDomainItemDto;
import com.sprinklr.sprintplanning.analytics.dto.DevSubDomainMetricsDto;
import com.sprinklr.sprintplanning.analytics.dto.WorkflowStageDistributionDto;
import com.sprinklr.sprintplanning.common.enums.Domain;
import com.sprinklr.sprintplanning.common.enums.StatusCategory;
import com.sprinklr.sprintplanning.common.model.DevSubDomainConfig;
import com.sprinklr.sprintplanning.common.model.DomainAllocation;
import com.sprinklr.sprintplanning.common.model.IssueView;
import com.sprinklr.sprintplanning.common.model.JiraFieldConfig;
import com.sprinklr.sprintplanning.common.model.WorkflowAnalysisConfig;
import com.sprinklr.sprintplanning.common.model.WorkflowAnalysisSection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowAnalyticsCalculatorTest {

  private WorkflowAnalyticsCalculator calculator;
  private JiraFieldConfig fieldConfig;

  @BeforeEach
  void setUp() {
    calculator = new WorkflowAnalyticsCalculator(new WorkflowSectionResolver());
    fieldConfig = new JiraFieldConfig(
        "customfield_10016",
        "customfield_10109",
        "customfield_10020",
        Map.of(),
        List.of("Bug"),
        List.of("Story"),
        Map.of(),
        Map.of("BE", "customfield_10144", "UI", "customfield_10146", "AI", "customfield_10145"),
        "customfield_10143",
        Map.of("BE", "Be", "UI", "Ui", "AI", "Ai"),
        null,
        sampleWorkflowConfig());
  }

  @Test
  void calculatesStageDistributionByCurrentSection() {
    List<IssueView> issues = List.of(
        issue("I-1", "REQUIREMENT", List.of()),
        issue("I-2", "NEED CONFIRMATION", List.of()),
        issue("I-3", "BACKLOG(DESIGN)", List.of()),
        issue("I-4", "DESIGN IN PROGRESS", List.of()),
        issue("I-5", "DESIGN REVIEW", List.of()),
        issue("I-6", "BACKLOG(DEV)", List.of()),
        issue("I-7", "IN PROGRESS", eng(Domain.BE, 3, false)),
        issue("I-8", "IN DEV REVIEW", eng(Domain.BE, 2, true)),
        issue("I-9", "QA IN PROGRESS", eng(Domain.BE, 4, false)),
        issue("I-10", "CLOSED", eng(Domain.BE, 1, false)));

    WorkflowStageDistributionDto distribution = calculator.calculateStageDistribution(issues, fieldConfig);

    assertThat(distribution.totalIssues()).isEqualTo(10);
    assertThat(sectionCount(distribution, "IN_REQUIREMENT")).isEqualTo(2);
    assertThat(sectionCount(distribution, "IN_DESIGN")).isEqualTo(3);
    assertThat(sectionCount(distribution, "DEV_BACKLOG")).isEqualTo(1);
    assertThat(sectionCount(distribution, "DEV_IN_PROGRESS")).isEqualTo(2);
    assertThat(sectionCount(distribution, "IN_QA")).isEqualTo(1);
    assertThat(sectionCount(distribution, "COMPLETED")).isEqualTo(1);
  }

  @Test
  void calculatesDevSubDomainIssueAndStoryPointCompletion() {
    List<IssueView> issues = List.of(
        issue("I-1", "BACKLOG(DESIGN)", List.of()),
        issue("I-2", "DESIGN IN PROGRESS", List.of()),
        issue("I-3", "REQUIREMENT", List.of()),
        issue("I-4", "BACKLOG(DEV)", eng(Domain.BE, 2, false)),
        issue("I-5", "IN PROGRESS", eng(Domain.BE, 3, false)),
        issue("I-6", "IN DEV REVIEW", eng(Domain.BE, 5, true)),
        issue("I-7", "QA IN PROGRESS", eng(Domain.BE, 4, false)),
        issue("I-8", "CLOSED", eng(Domain.BE, 6, false)),
        issue("I-9", "IN PROGRESS", eng(Domain.UI, 2, true)),
        issue("I-10", "IN PROGRESS", List.of()));

    DevSubDomainMetricsDto metrics = calculator.calculateDevSubDomainMetrics(issues, fieldConfig);

    assertThat(metrics.subDomainPoolIssueCount()).isEqualTo(7);

    DevSubDomainItemDto be = findSubDomain(metrics, Domain.BE);
    assertThat(be.applicableIssueCount()).isEqualTo(5);
    assertThat(be.completedIssueCount()).isEqualTo(3);
    assertThat(be.issueCompletionRatio()).isEqualTo(60.0);
    assertThat(be.totalStoryPoints()).isEqualTo(20.0);
    assertThat(be.completedStoryPoints()).isEqualTo(15.0);
    assertThat(be.storyPointCompletionRatio()).isEqualTo(75.0);

    DevSubDomainItemDto ui = findSubDomain(metrics, Domain.UI);
    assertThat(ui.applicableIssueCount()).isEqualTo(1);
    assertThat(ui.completedIssueCount()).isEqualTo(1);
  }

  private static int sectionCount(WorkflowStageDistributionDto distribution, String key) {
    return distribution.sections().stream()
        .filter(section -> section.key().equals(key))
        .mapToInt(section -> section.count())
        .findFirst()
        .orElse(0);
  }

  private static DevSubDomainItemDto findSubDomain(DevSubDomainMetricsDto metrics, Domain domain) {
    return metrics.subDomains().stream()
        .filter(item -> item.domain() == domain)
        .findFirst()
        .orElseThrow();
  }

  private static IssueView issue(String key, String status, List<DomainAllocation> engineeringAllocations) {
    return issue(key, status, engineeringAllocations, List.of());
  }

  private static IssueView issue(
      String key,
      String status,
      List<DomainAllocation> engineeringAllocations,
      List<DomainAllocation> domainAllocations) {
    return new IssueView(
        key,
        "Summary",
        Domain.UNKNOWN,
        null,
        "Story",
        status,
        StatusCategory.IN_PROGRESS,
        domainAllocations,
        engineeringAllocations);
  }

  @Test
  void countsSubDomainsFromDomainDropdownCombinations() {
    List<IssueView> issues = List.of(
        issue("I-1", "BACKLOG(DESIGN)", List.of()),
        issue("I-2", "BACKLOG(DEV)", List.of(
            new DomainAllocation(Domain.BE, 2, false),
            new DomainAllocation(Domain.UI, 1, false),
            new DomainAllocation(Domain.AI, 1, false))),
        issue("I-3", "IN PROGRESS", List.of(new DomainAllocation(Domain.BE, 3, false))),
        issue("I-4", "QA IN PROGRESS", List.of(
            new DomainAllocation(Domain.BE, 4, false),
            new DomainAllocation(Domain.UI, 2, false))),
        issue("I-5", "CLOSED", List.of(new DomainAllocation(Domain.UI, 5, false))));

    DevSubDomainMetricsDto metrics = calculator.calculateDevSubDomainMetrics(issues, fieldConfig);

    assertThat(metrics.subDomainPoolIssueCount()).isEqualTo(4);

    DevSubDomainItemDto be = findSubDomain(metrics, Domain.BE);
    assertThat(be.applicableIssueCount()).isEqualTo(3);
    assertThat(be.completedIssueCount()).isEqualTo(1);

    DevSubDomainItemDto ui = findSubDomain(metrics, Domain.UI);
    assertThat(ui.applicableIssueCount()).isEqualTo(3);
    assertThat(ui.completedIssueCount()).isEqualTo(2);
  }

  private static List<DomainAllocation> eng(Domain domain, double points, boolean completed) {
    return List.of(new DomainAllocation(domain, points, completed));
  }

  private static WorkflowAnalysisConfig sampleWorkflowConfig() {
    return new WorkflowAnalysisConfig(
        List.of(
            new WorkflowAnalysisSection("IN_REQUIREMENT", "In Requirement",
                List.of("REQUIREMENT", "NEED CONFIRMATION")),
            new WorkflowAnalysisSection("IN_DESIGN", "In Design",
                List.of("BACKLOG(DESIGN)", "DESIGN IN PROGRESS", "DESIGN REVIEW")),
            new WorkflowAnalysisSection("DEV_BACKLOG", "Dev Backlog",
                List.of("BACKLOG(DEV)", "QA SCENARIOS", "QA REJECTED")),
            new WorkflowAnalysisSection("DEV_IN_PROGRESS", "Dev In Progress",
                List.of("IN PROGRESS", "IN DEV REVIEW")),
            new WorkflowAnalysisSection("IN_QA", "In QA",
                List.of(
                    "WAITING FOR BUILD",
                    "READY FOR QA",
                    "QA IN PROGRESS",
                    "QA BLOCKED",
                    "WAITING FOR BUG FIXES")),
            new WorkflowAnalysisSection("COMPLETED", "Completed",
                List.of(
                    "QA VERIFIED(MAIN)",
                    "READY FOR MERGE",
                    "MERGED",
                    "READY FOR PRODUCTION",
                    "CLOSED",
                    "ARCHIVE"))),
        new DevSubDomainConfig(
            List.of("DEV_BACKLOG", "DEV_IN_PROGRESS", "IN_QA", "COMPLETED"),
            List.of("IN_QA", "COMPLETED"),
            Map.of("BE", "Be", "UI", "Ui", "AI", "Ai")));
  }
}
