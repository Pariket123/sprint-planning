package com.sprinklr.sprintplanning.analytics.calculator;

import com.sprinklr.sprintplanning.analytics.dto.AnalyticsResponse;
import com.sprinklr.sprintplanning.common.enums.Domain;
import com.sprinklr.sprintplanning.common.enums.StatusCategory;
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
  void returnsEmptyMetricsForNoIssues() {
    AnalyticsResponse response = calculator.calculate(1L, "Empty Sprint", List.of(), fieldConfig);

    assertThat(response.totalStoryPoints()).isZero();
    assertThat(response.issueCounts().total()).isZero();
    assertThat(response.statusDistribution()).isEmpty();
    assertThat(response.domainBreakdown()).isEmpty();
  }
}
