package com.sprinklr.sprintplanning.client.jira.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprinklr.sprintplanning.client.jira.dto.JiraIssueDto;
import com.sprinklr.sprintplanning.common.enums.Domain;
import com.sprinklr.sprintplanning.common.enums.StatusCategory;
import com.sprinklr.sprintplanning.common.model.JiraFieldConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JiraIssueMappingHelperTest {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final JiraIssueMappingHelper helper = new JiraIssueMappingHelper();
  private JiraFieldConfig fieldConfig;

  @BeforeEach
  void setUp() {
    fieldConfig = new JiraFieldConfig(
        "customfield_10016",
        "customfield_10020",
        Map.of("DEV", "Dev", "QA", "QA", "DESIGN", "Design"),
        List.of("Bug"),
        List.of("Story"));
  }

  @Test
  void resolvesCustomFields() throws Exception {
    JiraIssueDto issue = readIssue("""
        {
          "key": "WFM-1",
          "fields": {
            "summary": "Forecast model",
            "issuetype": { "name": "Story" },
            "status": {
              "name": "In Progress",
              "statusCategory": { "key": "indeterminate" }
            },
            "customfield_10016": 5,
            "customfield_10020": { "value": "Dev" }
          }
        }
        """);

    assertThat(helper.resolveSummary(issue)).isEqualTo("Forecast model");
    assertThat(helper.resolveIssueType(issue)).isEqualTo("Story");
    assertThat(helper.resolveStatus(issue)).isEqualTo("In Progress");
    assertThat(helper.resolveStatusCategory(issue)).isEqualTo(StatusCategory.IN_PROGRESS);
    assertThat(helper.resolveStoryPoints(issue, fieldConfig)).isEqualTo(5.0);
    assertThat(helper.resolveDomain(issue, fieldConfig)).isEqualTo(Domain.DEV);
  }

  @Test
  void resolvesUnknownWhenFieldsMissing() throws Exception {
    JiraIssueDto issue = readIssue("""
        {
          "key": "WFM-2",
          "fields": {
            "summary": "Untagged",
            "issuetype": { "name": "Task" },
            "status": {
              "name": "To Do",
              "statusCategory": { "key": "new" }
            }
          }
        }
        """);

    assertThat(helper.resolveDomain(issue, fieldConfig)).isEqualTo(Domain.UNKNOWN);
    assertThat(helper.resolveStoryPoints(issue, fieldConfig)).isNull();
    assertThat(helper.resolveStatusCategory(issue)).isEqualTo(StatusCategory.TODO);
  }

  private JiraIssueDto readIssue(String json) throws Exception {
    return objectMapper.readValue(json, JiraIssueDto.class);
  }
}
