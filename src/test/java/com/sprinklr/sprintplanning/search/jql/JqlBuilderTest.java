package com.sprinklr.sprintplanning.search.jql;

import com.sprinklr.sprintplanning.common.model.JiraFieldConfig;
import com.sprinklr.sprintplanning.search.dto.IssueSearchFilters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class JqlBuilderTest {

  private JqlBuilder jqlBuilder;
  private JiraFieldConfig fieldConfig;

  @BeforeEach
  void setUp() {
    jqlBuilder = new JqlBuilder();
    fieldConfig = new JiraFieldConfig(
        "customfield_10016",
        "customfield_10109",
        "customfield_10020",
        Map.of("BE", "Backend", "UI", "Frontend"),
        List.of("Bug"),
        List.of("Story"));
  }

  @Test
  void buildsProjectAndIssueTypeClause() {
    IssueSearchFilters filters = new IssueSearchFilters(
        List.of("Story", "Bug"),
        null, null, null, null, null, null, null, null, null, null, null);

    Optional<String> jql = jqlBuilder.build(List.of("WFM", "CARE"), filters, fieldConfig);

    assertThat(jql).isPresent();
    assertThat(jql.get())
        .isEqualTo("project IN (\"WFM\", \"CARE\") AND issuetype IN (\"Story\", \"Bug\") ORDER BY updated DESC");
  }

  @Test
  void buildsFixVersionInClausesEvenWhenCustomFieldWasConfigured() {
    JiraFieldConfig configWithLegacyCustomFixVersion = new JiraFieldConfig(
        "customfield_10016",
        "customfield_10109",
        "customfield_10020",
        Map.of("BE", "Backend", "UI", "Frontend"),
        List.of("Bug"),
        List.of("Story"),
        Map.of(),
        Map.of(),
        null,
        Map.of(),
        "customfield_10183");

    IssueSearchFilters filters = new IssueSearchFilters(
        null, null, null, List.of(12L, 13L),
        List.of("Q3 2026"), List.of("Deprecated"),
        null, null, null, null, null, null);

    Optional<String> jql = jqlBuilder.build(List.of("WFM"), filters, configWithLegacyCustomFixVersion);

    assertThat(jql).isPresent();
    assertThat(jql.get()).contains("fixVersion IN (\"Q3 2026\")");
    assertThat(jql.get()).contains("fixVersion NOT IN (\"Deprecated\")");
    assertThat(jql.get()).contains("sprint IN (12, 13)");
  }

  @Test
  void buildsFixVersionAndSprintClauses() {
    IssueSearchFilters filters = new IssueSearchFilters(
        null, null, null, List.of(12L, 13L),
        List.of("Q3 2026"), List.of("Deprecated"),
        null, null, null, null, null, null);

    Optional<String> jql = jqlBuilder.build(List.of("WFM"), filters, fieldConfig);

    assertThat(jql).isPresent();
    assertThat(jql.get()).contains("fixVersion IN (\"Q3 2026\")");
    assertThat(jql.get()).contains("fixVersion NOT IN (\"Deprecated\")");
    assertThat(jql.get()).contains("sprint IN (12, 13)");
  }

  @Test
  void buildsDomainClauseFromConfiguredValues() {
    IssueSearchFilters filters = new IssueSearchFilters(
        null, null, List.of("BE", "UI"), null, null, null, null, null, null, null, null, null);

    Optional<String> jql = jqlBuilder.build(List.of("WFM"), filters, fieldConfig);

    assertThat(jql).isPresent();
    assertThat(jql.get()).contains("cf[10109] IN (\"Backend\", \"Frontend\")");
  }

  @Test
  void buildsIssueKeyClause() {
    IssueSearchFilters filters = new IssueSearchFilters(
        null, null, null, null, null, null, null, null,
        List.of("CARE-105613", "CARE-105614"), null, null, null);

    Optional<String> jql = jqlBuilder.build(List.of("CARE"), filters, fieldConfig);

    assertThat(jql).isPresent();
    assertThat(jql.get()).contains("key IN (\"CARE-105613\", \"CARE-105614\")");
  }

  @Test
  void returnsEmptyWhenDomainFilterCannotBeApplied() {
    IssueSearchFilters filters = new IssueSearchFilters(
        null, null, List.of("BE"), null, null, null, null, null, null, null, null, null);

    Optional<String> jql = jqlBuilder.build(List.of("WFM"), filters, null);

    assertThat(jql).isEmpty();
  }

  @Test
  void escapesJqlSpecialCharacters() {
    assertThat(jqlBuilder.escapeJqlString("Q3 \"Beta\"")).isEqualTo("Q3 \\\"Beta\\\"");
  }
}
