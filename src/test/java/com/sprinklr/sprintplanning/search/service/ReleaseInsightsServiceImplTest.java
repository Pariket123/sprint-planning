package com.sprinklr.sprintplanning.search.service;

import com.sprinklr.sprintplanning.analytics.calculator.AnalyticsCalculator;
import com.sprinklr.sprintplanning.analytics.dto.AnalyticsResponse;
import com.sprinklr.sprintplanning.analytics.workflow.WorkflowAnalyticsCalculator;
import com.sprinklr.sprintplanning.analytics.workflow.WorkflowSectionResolver;
import com.sprinklr.sprintplanning.client.jira.JiraClient;
import com.sprinklr.sprintplanning.common.enums.Domain;
import com.sprinklr.sprintplanning.common.enums.StatusCategory;
import com.sprinklr.sprintplanning.common.model.IssueView;
import com.sprinklr.sprintplanning.common.model.JiraFieldConfig;
import com.sprinklr.sprintplanning.planning.calculator.CapacityAllocationCalculator;
import com.sprinklr.sprintplanning.planning.calculator.PlanningCalculator;
import com.sprinklr.sprintplanning.planning.config.PlanningProperties;
import com.sprinklr.sprintplanning.release.model.ReleaseConfigDocument;
import com.sprinklr.sprintplanning.release.service.ReleaseService;
import com.sprinklr.sprintplanning.search.FilterMergeHelper;
import com.sprinklr.sprintplanning.search.dto.IssueSearchReleaseRequest;
import com.sprinklr.sprintplanning.search.jql.JqlBuilder;
import com.sprinklr.sprintplanning.search.jql.JqlMergeHelper;
import com.sprinklr.sprintplanning.search.jql.ReleaseJqlResolver;
import com.sprinklr.sprintplanning.search.support.PodProjectKeyResolver;
import com.sprinklr.sprintplanning.team.mapper.JiraConfigMapper;
import com.sprinklr.sprintplanning.team.model.PodDocument;
import com.sprinklr.sprintplanning.team.model.PodJiraConfig;
import com.sprinklr.sprintplanning.team.service.TeamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReleaseInsightsServiceImplTest {

  @Mock
  private TeamService teamService;
  @Mock
  private ReleaseService releaseService;
  @Mock
  private JiraClient jiraClient;
  @Mock
  private JiraConfigMapper jiraConfigMapper;

  private ReleaseInsightsService releaseInsightsService;

  @BeforeEach
  void setUp() {
    ReleaseJqlResolver releaseJqlResolver = new ReleaseJqlResolver(
        new JqlBuilder(),
        new JqlMergeHelper(),
        new FilterMergeHelper(),
        new PodProjectKeyResolver());
    releaseInsightsService = new ReleaseInsightsServiceImpl(
        teamService,
        releaseService,
        jiraClient,
        jiraConfigMapper,
        releaseJqlResolver,
        new AnalyticsCalculator(new WorkflowAnalyticsCalculator(new WorkflowSectionResolver())),
        new PlanningCalculator(new PlanningProperties(), new CapacityAllocationCalculator()));
  }

  @Test
  void analyzeReleaseUsesMergedJqlAndReturnsDomainBreakdown() {
    ReleaseConfigDocument release = releaseWithBaseJql("project = SCRUM AND fixVersion = \"Q3\"");
    release.setName("Q3 Release");

    PodDocument pod = podWithProjects("pod-1", List.of("SCRUM"));
    JiraFieldConfig fieldConfig = fieldConfig();

    when(teamService.getActivePodDocument("pod-1")).thenReturn(pod);
    when(releaseService.getActiveReleaseDocument("pod-1", "release-1")).thenReturn(release);
    when(jiraConfigMapper.toJiraFieldConfig(any())).thenReturn(fieldConfig);
    when(jiraClient.searchAllIssues(any(), eq(fieldConfig)))
        .thenReturn(List.of(
            new IssueView("SCRUM-1", "Done", Domain.DEV, 5.0, "Story", "Done", StatusCategory.DONE),
            new IssueView("SCRUM-2", "Todo", Domain.QA, 3.0, "Bug", "To Do", StatusCategory.TODO)));

    AnalyticsResponse result = releaseInsightsService.analyzeRelease(
        "pod-1",
        "release-1",
        new IssueSearchReleaseRequest("status != Done", null));

    assertThat(result.sprintName()).isEqualTo("Q3 Release");
    assertThat(result.issueCounts().total()).isEqualTo(1);
    assertThat(result.domainBreakdown()).extracting("domain").containsExactly(Domain.DEV);
    verify(jiraClient).searchAllIssues(
        eq("(project = SCRUM AND fixVersion = \"Q3\") AND (status != Done)"),
        eq(fieldConfig));
  }

  @Test
  void analyzeReleaseFiltersIssuesByBugProfile() {
    ReleaseConfigDocument release = releaseWithBaseJql("project = SCRUM AND fixVersion = \"Q3\"");
    release.setName("Q3 Release");

    PodDocument pod = podWithProjects("pod-1", List.of("SCRUM"));
    JiraFieldConfig fieldConfig = fieldConfig();

    when(teamService.getActivePodDocument("pod-1")).thenReturn(pod);
    when(releaseService.getActiveReleaseDocument("pod-1", "release-1")).thenReturn(release);
    when(jiraConfigMapper.toJiraFieldConfig(any())).thenReturn(fieldConfig);
    when(jiraClient.searchAllIssues(any(), eq(fieldConfig)))
        .thenReturn(List.of(
            new IssueView("SCRUM-1", "Done", Domain.DEV, 5.0, "Story", "Done", StatusCategory.DONE),
            new IssueView("SCRUM-2", "Todo", Domain.QA, 3.0, "Bug", "To Do", StatusCategory.TODO)));

    AnalyticsResponse result = releaseInsightsService.analyzeRelease(
        "pod-1",
        "release-1",
        new IssueSearchReleaseRequest(null, "bug"));

    assertThat(result.issueCounts().total()).isEqualTo(1);
    assertThat(result.domainBreakdown()).extracting("domain").containsExactly(Domain.QA);
  }

  private ReleaseConfigDocument releaseWithBaseJql(String baseJql) {
    ReleaseConfigDocument release = new ReleaseConfigDocument();
    release.setId("release-1");
    release.setTeamId("team-1");
    release.setBaseJql(baseJql);
    return release;
  }

  private PodDocument podWithProjects(String podId, List<String> projectKeys) {
    PodDocument pod = new PodDocument();
    pod.setId(podId);
    PodJiraConfig jiraConfig = new PodJiraConfig();
    jiraConfig.setProjectKeys(projectKeys);
    pod.setJiraConfig(jiraConfig);
    return pod;
  }

  private JiraFieldConfig fieldConfig() {
    return new JiraFieldConfig(
        "customfield_10016",
        "customfield_10109",
        "customfield_10020",
        Map.of("BE", "Backend"),
        List.of("Bug"),
        List.of("Story"));
  }
}
