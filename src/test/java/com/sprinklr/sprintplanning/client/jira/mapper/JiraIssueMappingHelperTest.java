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
        "customfield_10109",
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
            "customfield_10109": { "value": "Dev" }
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

  @Test
  void resolvesSearchViewFields() throws Exception {
    JiraIssueDto issue = readIssue("""
        {
          "key": "WFM-3",
          "fields": {
            "summary": "Searchable issue",
            "issuetype": { "name": "Bug" },
            "status": {
              "name": "Done",
              "statusCategory": { "key": "done" }
            },
            "assignee": {
              "accountId": "user-1",
              "displayName": "Jane Doe"
            },
            "priority": { "name": "High" },
            "fixVersions": [{ "name": "Q3 2026" }, { "name": "Release-12.4" }],
            "labels": ["backend", "urgent"],
            "components": [{ "name": "API" }],
            "customfield_10020": [
              { "id": 37, "name": "Sprint 12", "state": "closed" },
              { "id": 42, "name": "Sprint 13", "state": "active" }
            ],
            "customfield_10016": 3,
            "customfield_10109": { "value": "QA" }
          }
        }
        """);

    assertThat(helper.resolveAssigneeId(issue)).isEqualTo("user-1");
    assertThat(helper.resolveAssigneeDisplayName(issue)).isEqualTo("Jane Doe");
    assertThat(helper.resolvePriority(issue)).isEqualTo("High");
    assertThat(helper.resolveFixVersions(issue, null)).containsExactly("Q3 2026", "Release-12.4");
    assertThat(helper.resolveSprintIds(issue, fieldConfig)).containsExactly(37L, 42L);
    assertThat(helper.resolveCurrentSprintId(issue, fieldConfig)).isEqualTo(42L);
    assertThat(helper.resolveLabels(issue)).containsExactly("backend", "urgent");
    assertThat(helper.resolveComponents(issue)).containsExactly("API");
    assertThat(helper.resolveDomain(issue, fieldConfig)).isEqualTo(Domain.QA);
  }

  @Test
  void resolvesSprintIdsFromGreenhopperStringFormat() throws Exception {
    JiraIssueDto issue = readIssue("""
        {
          "key": "SCRUM-6",
          "fields": {
            "summary": "Analytics Service",
            "issuetype": { "name": "Story" },
            "status": {
              "name": "To Do",
              "statusCategory": { "key": "new" }
            },
            "customfield_10020": [
              "com.atlassian.greenhopper.service.sprint.Sprint@1a2b3c[id=3,rapidViewId=1,state=FUTURE,name=SCRUM Sprint 1,startDate=<null>,endDate=<null>,completeDate=<null>,sequence=2]"
            ]
          }
        }
        """);

    assertThat(helper.resolveSprintIds(issue, fieldConfig)).containsExactly(3L);
    assertThat(helper.resolveCurrentSprintId(issue, fieldConfig)).isEqualTo(3L);
  }

  @Test
  void resolvesEmptySearchFieldsWhenMissing() throws Exception {
    JiraIssueDto issue = readIssue("""
        {
          "key": "WFM-4",
          "fields": {
            "summary": "Minimal",
            "issuetype": { "name": "Task" },
            "status": {
              "name": "To Do",
              "statusCategory": { "key": "new" }
            }
          }
        }
        """);

    assertThat(helper.resolveAssigneeId(issue)).isNull();
    assertThat(helper.resolveAssigneeDisplayName(issue)).isNull();
    assertThat(helper.resolvePriority(issue)).isNull();
    assertThat(helper.resolveFixVersions(issue, null)).isEmpty();
    assertThat(helper.resolveSprintIds(issue, fieldConfig)).isEmpty();
    assertThat(helper.resolveLabels(issue)).isEmpty();
    assertThat(helper.resolveComponents(issue)).isEmpty();
  }

  @Test
  void resolvesSprintIdsFromConfiguredFieldId() throws Exception {
    JiraFieldConfig config = new JiraFieldConfig(
        "customfield_10016",
        "customfield_10109",
        "customfield_10042",
        Map.of("DEV", "Dev"),
        List.of("Bug"),
        List.of("Story"));

    JiraIssueDto issue = readIssue("""
        {
          "key": "WFM-5",
          "fields": {
            "summary": "Sprint issue",
            "issuetype": { "name": "Story" },
            "status": {
              "name": "To Do",
              "statusCategory": { "key": "new" }
            },
            "customfield_10042": [
              { "id": 7, "name": "SCRUM Sprint 1", "state": "future" }
            ]
          }
        }
        """);

    assertThat(helper.resolveSprintIds(issue, config)).containsExactly(7L);
  }

  @Test
  void resolvesCustomFixVersionFieldWhenConfigured() throws Exception {
    JiraFieldConfig configWithFixVersion = new JiraFieldConfig(
        "customfield_10016",
        "customfield_10109",
        "customfield_10020",
        Map.of("DEV", "Dev"),
        List.of("Bug"),
        List.of("Story"),
        Map.of(),
        Map.of(),
        null,
        Map.of(),
        "customfield_10183");

    JiraIssueDto issue = readIssue("""
        {
          "key": "SCRUM-7",
          "fields": {
            "summary": "Release scoped",
            "issuetype": { "name": "Story" },
            "status": {
              "name": "To Do",
              "statusCategory": { "key": "new" }
            },
            "customfield_10183": "Q3 2026"
          }
        }
        """);

    assertThat(helper.resolveFixVersions(issue, configWithFixVersion)).containsExactly("Q3 2026");
    assertThat(helper.resolveFixVersions(issue, null)).isEmpty();
  }

  @Test
  void usesPerDomainStoryPointsWhenConfiguredForDomain() throws Exception {
    JiraFieldConfig multiDomainConfig = new JiraFieldConfig(
        "customfield_10016",
        "customfield_10109",
        "customfield_10020",
        Map.of("DEV", "Dev", "BE", "Backend"),
        List.of("Bug"),
        List.of("Story"),
        Map.of(),
        Map.of(
            "DEV", "customfield_10179",
            "BE", "customfield_10144",
            "UI", "customfield_10146",
            "AI", "customfield_10145"),
        "customfield_10143",
        Map.of("BE", "Be", "UI", "Ui", "AI", "Ai"));

    JiraIssueDto devIssue = readIssue("""
        {
          "key": "SCRUM-1",
          "fields": {
            "summary": "Dev task",
            "issuetype": { "name": "Story" },
            "status": {
              "name": "Done",
              "statusCategory": { "key": "done" }
            },
            "customfield_10109": { "value": "Dev" },
            "customfield_10016": 99,
            "customfield_10179": 3
          }
        }
        """);

    JiraIssueDto backendIssue = readIssue("""
        {
          "key": "SCRUM-2",
          "fields": {
            "summary": "Backend task",
            "issuetype": { "name": "Story" },
            "status": {
              "name": "In Progress",
              "statusCategory": { "key": "indeterminate" }
            },
            "customfield_10109": { "value": "Backend" },
            "customfield_10016": 99,
            "customfield_10144": 4
          }
        }
        """);

    assertThat(helper.resolveDomainAllocations(devIssue, multiDomainConfig))
        .containsExactly(
            new com.sprinklr.sprintplanning.common.model.DomainAllocation(Domain.DEV, 3.0, false));
    assertThat(helper.resolveStoryPoints(devIssue, multiDomainConfig)).isEqualTo(3.0);
    assertThat(helper.resolveDomainAllocations(backendIssue, multiDomainConfig))
        .containsExactly(
            new com.sprinklr.sprintplanning.common.model.DomainAllocation(Domain.BE, 4.0, false));
    assertThat(helper.resolveStoryPoints(backendIssue, multiDomainConfig)).isEqualTo(4.0);
  }

  @Test
  void resolvesMultiDomainAllocationsWithMissingPerDomainStoryPointsAsZero() throws Exception {
    JiraFieldConfig multiDomainConfig = new JiraFieldConfig(
        "customfield_10016",
        "customfield_10109",
        "customfield_10020",
        Map.of("BE", "Backend", "UI", "Ui"),
        List.of("Bug"),
        List.of("Story"),
        Map.of(),
        Map.of("BE", "customfield_10144", "UI", "customfield_10146"),
        "customfield_10143",
        Map.of("BE", "Be", "UI", "Ui"));

    JiraIssueDto issue = readIssue("""
        {
          "key": "WFM-11",
          "fields": {
            "summary": "Backend only",
            "issuetype": { "name": "Story" },
            "status": {
              "name": "To Do",
              "statusCategory": { "key": "new" }
            },
            "customfield_10109": { "value": "Backend" }
          }
        }
        """);

    assertThat(helper.resolveDomainAllocations(issue, multiDomainConfig))
        .containsExactly(
            new com.sprinklr.sprintplanning.common.model.DomainAllocation(Domain.BE, 0.0, false));
    assertThat(helper.resolveStoryPoints(issue, multiDomainConfig)).isZero();
  }

  @Test
  void resolvesMultiDomainAllocationsFromPerDomainFieldsAndCompletionCheckboxes() throws Exception {
    JiraFieldConfig multiDomainConfig = new JiraFieldConfig(
        "customfield_10016",
        "customfield_10109",
        "customfield_10020",
        Map.of("BE", "Backend", "UI", "Ui", "AI", "Ai"),
        List.of("Bug"),
        List.of("Story"),
        Map.of("BE+UI", "Backend+Ui"),
        Map.of("BE", "customfield_10144", "UI", "customfield_10146", "AI", "customfield_10145"),
        "customfield_10143",
        Map.of("BE", "Be", "UI", "Ui", "AI", "Ai"));

    JiraIssueDto issue = readIssue("""
        {
          "key": "WFM-10",
          "fields": {
            "summary": "Composite issue",
            "issuetype": { "name": "Story" },
            "status": {
              "name": "In Progress",
              "statusCategory": { "key": "indeterminate" }
            },
            "customfield_10109": { "value": "Backend+Ui" },
            "customfield_10144": 3,
            "customfield_10146": 2,
            "customfield_10143": [{ "value": "Be" }]
          }
        }
        """);

    assertThat(helper.resolveStoryPoints(issue, multiDomainConfig)).isEqualTo(5.0);
    assertThat(helper.resolveDomain(issue, multiDomainConfig)).isEqualTo(Domain.BE);
    assertThat(helper.resolveDomainAllocations(issue, multiDomainConfig))
        .containsExactly(
            new com.sprinklr.sprintplanning.common.model.DomainAllocation(Domain.BE, 3.0, true),
            new com.sprinklr.sprintplanning.common.model.DomainAllocation(Domain.UI, 2.0, false));
  }

  @Test
  void resolvesEngineeringAllocationsFromDomainDropdownCombinations() throws Exception {
    JiraFieldConfig config = new JiraFieldConfig(
        "customfield_10016",
        "customfield_10109",
        "customfield_10020",
        Map.of("BE", "Be", "UI", "Ui", "AI", "Ai"),
        List.of("Bug"),
        List.of("Story"),
        Map.of(
            "BE+UI", "Be+Ui",
            "BE+AI", "Be+Ai",
            "BE+UI+AI", "Be+Ui+Ai",
            "AI+UI", "Ai+Ui"),
        Map.of("BE", "customfield_10144", "UI", "customfield_10146", "AI", "customfield_10145"),
        "customfield_10143",
        Map.of("BE", "Be", "UI", "Ui", "AI", "Ai"));

    JiraIssueDto issue = readIssue("""
        {
          "key": "SCRUM-20",
          "fields": {
            "summary": "Full stack",
            "issuetype": { "name": "Story" },
            "status": {
              "name": "IN PROGRESS",
              "statusCategory": { "key": "indeterminate" }
            },
            "customfield_10109": { "value": "Be+Ui+Ai" },
            "customfield_10144": 3,
            "customfield_10146": 2,
            "customfield_10145": 1,
            "customfield_10143": [{ "value": "Be" }]
          }
        }
        """);

    assertThat(helper.resolveEngineeringAllocations(issue, config))
        .containsExactly(
            new com.sprinklr.sprintplanning.common.model.DomainAllocation(Domain.BE, 3.0, true),
            new com.sprinklr.sprintplanning.common.model.DomainAllocation(Domain.UI, 2.0, false),
            new com.sprinklr.sprintplanning.common.model.DomainAllocation(Domain.AI, 1.0, false));
  }

  @Test
  void resolvesDomainAllocationsFromStageStoryPointFieldsAndEngineeringDropdown() throws Exception {
    JiraFieldConfig config = new JiraFieldConfig(
        "customfield_10016",
        "customfield_10109",
        "customfield_10020",
        Map.of("BE", "Be", "UI", "Ui", "AI", "Ai"),
        List.of("Bug"),
        List.of("Story"),
        Map.of("BE+UI", "Be+Ui"),
        Map.of(
            "DEV", "customfield_10179",
            "QA", "customfield_10181",
            "DESIGN", "customfield_10180",
            "BE", "customfield_10144",
            "UI", "customfield_10146",
            "AI", "customfield_10145"),
        "customfield_10143",
        Map.of("BE", "Be", "UI", "Ui", "AI", "Ai"));

    JiraIssueDto issue = readIssue("""
        {
          "key": "SCRUM-30",
          "fields": {
            "summary": "Workflow issue",
            "issuetype": { "name": "Story" },
            "status": {
              "name": "BACKLOG(DEV)",
              "statusCategory": { "key": "new" }
            },
            "customfield_10109": { "value": "Be+Ui" },
            "customfield_10179": 2,
            "customfield_10181": 3,
            "customfield_10180": 1,
            "customfield_10144": 5,
            "customfield_10146": 4
          }
        }
        """);

    assertThat(helper.resolveDomainAllocations(issue, config))
        .containsExactly(
            new com.sprinklr.sprintplanning.common.model.DomainAllocation(Domain.BE, 5.0, false),
            new com.sprinklr.sprintplanning.common.model.DomainAllocation(Domain.UI, 4.0, false),
            new com.sprinklr.sprintplanning.common.model.DomainAllocation(Domain.DEV, 2.0, false),
            new com.sprinklr.sprintplanning.common.model.DomainAllocation(Domain.QA, 3.0, false),
            new com.sprinklr.sprintplanning.common.model.DomainAllocation(Domain.DESIGN, 1.0, false));
    assertThat(helper.resolveStoryPoints(issue, config)).isEqualTo(15.0);
  }

  @Test
  void resolvesDomainLabelFromCompositeDomainField() throws Exception {
    JiraFieldConfig config = new JiraFieldConfig(
        "customfield_10016",
        "customfield_10109",
        "customfield_10020",
        Map.of("BE", "Be", "UI", "Ui", "AI", "Ai"),
        List.of("Bug"),
        List.of("Story"),
        Map.of("BE+UI", "Be+Ui"),
        Map.of(),
        null,
        Map.of());

    JiraIssueDto issue = readIssue("""
        {
          "key": "SCRUM-6",
          "fields": {
            "summary": "Analytics Service",
            "issuetype": { "name": "Story" },
            "status": {
              "name": "To Do",
              "statusCategory": { "key": "new" }
            },
            "customfield_10109": { "value": "Be+Ui" }
          }
        }
        """);

    assertThat(helper.resolveDomainLabel(issue, config)).isEqualTo("BE UI");
  }

  @Test
  void resolvesDomainLabelFromSingleDomainField() throws Exception {
    JiraIssueDto issue = readIssue("""
        {
          "key": "SCRUM-7",
          "fields": {
            "summary": "Backend task",
            "issuetype": { "name": "Task" },
            "status": {
              "name": "To Do",
              "statusCategory": { "key": "new" }
            },
            "customfield_10109": { "value": "Be" }
          }
        }
        """);

    JiraFieldConfig config = new JiraFieldConfig(
        "customfield_10016",
        "customfield_10109",
        "customfield_10020",
        Map.of("BE", "Be", "UI", "Ui", "AI", "Ai"),
        List.of("Bug"),
        List.of("Story"));

    assertThat(helper.resolveDomainLabel(issue, config)).isEqualTo("BE");
  }

  private JiraIssueDto readIssue(String json) throws Exception {
    return objectMapper.readValue(json, JiraIssueDto.class);
  }
}
