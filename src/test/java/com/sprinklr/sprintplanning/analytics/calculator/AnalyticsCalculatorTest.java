package com.sprinklr.sprintplanning.analytics.calculator;

import com.sprinklr.sprintplanning.analytics.dto.AnalyticsResponse;
import com.sprinklr.sprintplanning.common.enums.Domain;
import com.sprinklr.sprintplanning.common.enums.StatusCategory;
import com.sprinklr.sprintplanning.common.model.DomainAllocation;
import com.sprinklr.sprintplanning.common.model.IssueView;
import com.sprinklr.sprintplanning.common.model.JiraFieldConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AnalyticsCalculatorTest {

  private AnalyticsCalculator calculator;
  private JiraFieldConfig fieldConfig;

  @BeforeEach
  void setUp() {
    calculator = new AnalyticsCalculator();
    fieldConfig = new JiraFieldConfig(
        "customfield_10016",
        "customfield_10109",
        "customfield_10020",
        Map.of("DEV", "Dev", "QA", "QA"),
        List.of("Bug"),
        List.of("Story", "Task"));
  }

  @Test
  void aggregatesStoryPointsIssueCountsAndBreakdowns() {
    List<IssueView> issues = List.of(
        new IssueView("WFM-1", "Done story", Domain.DEV, 5.0, "Story", "Done", StatusCategory.DONE),
        new IssueView("WFM-2", "In progress bug", Domain.QA, 3.0, "Bug", "In Progress", StatusCategory.IN_PROGRESS),
        new IssueView("WFM-3", "Todo task", Domain.DEV, null, "Task", "To Do", StatusCategory.TODO));

    AnalyticsResponse response = calculator.calculate(42L, "Sprint 42", issues, fieldConfig);

    assertThat(response.jiraSprintId()).isEqualTo(42L);
    assertThat(response.sprintName()).isEqualTo("Sprint 42");
    assertThat(response.totalStoryPoints()).isEqualTo(8.0);
    assertThat(response.completedStoryPoints()).isEqualTo(5.0);
    assertThat(response.remainingStoryPoints()).isEqualTo(3.0);
    assertThat(response.issueCounts().total()).isEqualTo(3);
    assertThat(response.issueCounts().completed()).isEqualTo(1);
    assertThat(response.issueCounts().remaining()).isEqualTo(2);
    assertThat(response.bugsVsFeatures().bugs().count()).isEqualTo(1);
    assertThat(response.bugsVsFeatures().features().count()).isEqualTo(2);
    assertThat(response.bugsVsFeatures().bugs().storyPoints()).isEqualTo(3.0);
    assertThat(response.bugsVsFeatures().features().storyPoints()).isEqualTo(5.0);
    assertThat(response.statusDistribution()).hasSize(3);
    assertThat(response.domainBreakdown()).extracting("domain").containsExactly(Domain.DEV, Domain.QA);
  }

  @Test
  void calculatesDomainBreakdownPercentagesAndCompletionMetrics() {
    List<IssueView> issues = List.of(
        new IssueView("WFM-1", "Done 1", Domain.DEV, 5.0, "Story", "Done", StatusCategory.DONE),
        new IssueView("WFM-2", "Done 2", Domain.DEV, 5.0, "Story", "Done", StatusCategory.DONE),
        new IssueView("WFM-3", "Done 3", Domain.DEV, 5.0, "Story", "Done", StatusCategory.DONE),
        new IssueView("WFM-4", "Open 1", Domain.DEV, 5.0, "Story", "To Do", StatusCategory.TODO),
        new IssueView("WFM-5", "Open 2", Domain.DEV, 5.0, "Story", "To Do", StatusCategory.TODO),
        new IssueView("WFM-6", "QA done", Domain.QA, 4.0, "Bug", "Done", StatusCategory.DONE),
        new IssueView("WFM-7", "QA open", Domain.QA, 3.0, "Bug", "To Do", StatusCategory.TODO),
        new IssueView("WFM-8", "QA open 2", Domain.QA, 3.0, "Bug", "To Do", StatusCategory.TODO));

    AnalyticsResponse response = calculator.calculate(42L, "Sprint 42", issues, fieldConfig);

    assertThat(response.domainBreakdown()).filteredOn(item -> item.domain() == Domain.DEV).first()
        .satisfies(dev -> {
          assertThat(dev.count()).isEqualTo(5);
          assertThat(dev.storyPoints()).isEqualTo(25.0);
          assertThat(dev.issueCountPercentage()).isEqualTo(62.5);
          assertThat(dev.storyPointPercentage()).isEqualTo(71.43);
          assertThat(dev.completedIssueCount()).isEqualTo(3);
          assertThat(dev.completedStoryPoints()).isEqualTo(15.0);
          assertThat(dev.remainingIssueCount()).isEqualTo(2);
          assertThat(dev.remainingStoryPoints()).isEqualTo(10.0);
          assertThat(dev.issueCompletionPercentage()).isEqualTo(60.0);
          assertThat(dev.storyPointCompletionPercentage()).isEqualTo(60.0);
        });

    assertThat(response.domainBreakdown()).filteredOn(item -> item.domain() == Domain.QA).first()
        .satisfies(qa -> {
          assertThat(qa.count()).isEqualTo(3);
          assertThat(qa.storyPoints()).isEqualTo(10.0);
          assertThat(qa.issueCountPercentage()).isEqualTo(37.5);
          assertThat(qa.storyPointPercentage()).isEqualTo(28.57);
          assertThat(qa.completedIssueCount()).isEqualTo(1);
          assertThat(qa.completedStoryPoints()).isEqualTo(4.0);
          assertThat(qa.remainingIssueCount()).isEqualTo(2);
          assertThat(qa.remainingStoryPoints()).isEqualTo(6.0);
          assertThat(qa.issueCompletionPercentage()).isEqualTo(33.33);
          assertThat(qa.storyPointCompletionPercentage()).isEqualTo(40.0);
        });
  }

  @Test
  void countsJiraDoneIssuesAsCompletedEvenWhenDevCompletedCheckboxesAreUnset() {
    List<IssueView> issues = List.of(
        new IssueView(
            "SCRUM-1",
            "Done dev task",
            Domain.DEV,
            3.0,
            "Story",
            "Done",
            StatusCategory.DONE,
            List.of(new DomainAllocation(Domain.DEV, 3.0, false))),
        new IssueView(
            "SCRUM-2",
            "Backend in progress",
            Domain.BE,
            4.0,
            "Story",
            "In Progress",
            StatusCategory.IN_PROGRESS,
            List.of(new DomainAllocation(Domain.BE, 4.0, false))));

    AnalyticsResponse response = calculator.calculate(2L, "SCRUM Sprint 0", issues, fieldConfig);

    assertThat(response.issueCounts().total()).isEqualTo(2);
    assertThat(response.issueCounts().completed()).isEqualTo(1);
    assertThat(response.issueCounts().remaining()).isEqualTo(1);
    assertThat(response.domainBreakdown()).filteredOn(item -> item.domain() == Domain.DEV).first()
        .satisfies(dev -> {
          assertThat(dev.count()).isEqualTo(1);
          assertThat(dev.completedIssueCount()).isEqualTo(1);
        });
    assertThat(response.domainBreakdown()).filteredOn(item -> item.domain() == Domain.BE).first()
        .satisfies(be -> {
          assertThat(be.storyPoints()).isEqualTo(4.0);
          assertThat(be.completedStoryPoints()).isZero();
          assertThat(be.remainingStoryPoints()).isEqualTo(4.0);
        });
  }

  @Test
  void returnsEmptyMetricsForNoIssues() {
    AnalyticsResponse response = calculator.calculate(1L, "Empty Sprint", List.of(), fieldConfig);

    assertThat(response.totalStoryPoints()).isZero();
    assertThat(response.issueCounts().total()).isZero();
    assertThat(response.statusDistribution()).isEmpty();
    assertThat(response.domainBreakdown()).isEmpty();
  }
}
