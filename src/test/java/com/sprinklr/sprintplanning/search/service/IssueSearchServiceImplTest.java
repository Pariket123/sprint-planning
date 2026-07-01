package com.sprinklr.sprintplanning.search.service;

import com.sprinklr.sprintplanning.analytics.calculator.AnalyticsCalculator;
import com.sprinklr.sprintplanning.analytics.workflow.WorkflowAnalyticsCalculator;
import com.sprinklr.sprintplanning.analytics.workflow.WorkflowSectionResolver;
import com.sprinklr.sprintplanning.analytics.dto.AnalyticsResponse;
import com.sprinklr.sprintplanning.planning.calculator.CapacityAllocationCalculator;
import com.sprinklr.sprintplanning.planning.calculator.PlanningCalculator;
import com.sprinklr.sprintplanning.planning.config.PlanningProperties;
import com.sprinklr.sprintplanning.client.jira.JiraClient;
import com.sprinklr.sprintplanning.common.enums.Domain;
import com.sprinklr.sprintplanning.common.enums.StatusCategory;
import com.sprinklr.sprintplanning.common.exception.ResourceNotFoundException;
import com.sprinklr.sprintplanning.common.model.IssueSearchPage;
import com.sprinklr.sprintplanning.common.model.IssueView;
import com.sprinklr.sprintplanning.common.model.JiraFieldConfig;
import com.sprinklr.sprintplanning.release.model.ReleaseBasicFilters;
import com.sprinklr.sprintplanning.release.model.ReleaseConfigDocument;
import com.sprinklr.sprintplanning.release.service.ReleaseService;
import com.sprinklr.sprintplanning.search.FilterMergeHelper;
import com.sprinklr.sprintplanning.search.dto.IssueSearchFilters;
import com.sprinklr.sprintplanning.search.dto.IssueSearchPageDto;
import com.sprinklr.sprintplanning.search.dto.IssueSearchReleaseRequest;
import com.sprinklr.sprintplanning.search.dto.TicketViewDto;
import com.sprinklr.sprintplanning.search.jql.JqlBuilder;
import com.sprinklr.sprintplanning.search.jql.JqlMergeHelper;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IssueSearchServiceImplTest {

  @Mock
  private TeamService teamService;
  @Mock
  private ReleaseService releaseService;
  @Mock
  private JiraClient jiraClient;
  @Mock
  private JiraConfigMapper jiraConfigMapper;

  private IssueSearchService issueSearchService;

  @BeforeEach
  void setUp() {
    issueSearchService = new IssueSearchServiceImpl(
        teamService,
        releaseService,
        jiraClient,
        jiraConfigMapper,
        new JqlBuilder(),
        new JqlMergeHelper(),
        new FilterMergeHelper(),
        new AnalyticsCalculator(new WorkflowAnalyticsCalculator(new WorkflowSectionResolver())),
        new PlanningCalculator(new PlanningProperties(), new CapacityAllocationCalculator()));
  }

  @Test
  void searchInPodBuildsJqlAndReturnsPaginatedTickets() {
    PodDocument pod = podWithProjects("pod-1", List.of("WFM"));
    JiraFieldConfig fieldConfig = fieldConfig();

    when(teamService.getActivePodDocument("pod-1")).thenReturn(pod);
    when(jiraConfigMapper.toJiraFieldConfig(any())).thenReturn(fieldConfig);
    when(jiraClient.searchIssues(any(), eq(fieldConfig), eq(0), eq(50)))
        .thenReturn(searchPage(ticket("WFM-1")));

    IssueSearchFilters filters = new IssueSearchFilters(
        List.of("Story"), null, null, null, null, null, null, null, null, null, null, null);

    IssueSearchPageDto result = issueSearchService.searchInPod("pod-1", filters, 0, 50);

    assertThat(result.issues()).hasSize(1);
    assertThat(result.issues().get(0).key()).isEqualTo("WFM-1");
    verify(jiraClient).searchIssues(
        eq("project IN (\"WFM\") AND issuetype IN (\"Story\") ORDER BY updated DESC"),
        eq(fieldConfig),
        eq(0),
        eq(50));
  }

  @Test
  void searchInReleaseUsesBaseJqlOnlyWhenAdditionalIsEmpty() {
    ReleaseConfigDocument release = releaseWithBaseJql(
        "project = CARE AND fixVersion = \"Q3 2026\"");

    PodDocument pod = podWithProjects("pod-1", List.of("CARE"));
    JiraFieldConfig fieldConfig = fieldConfig();

    when(teamService.getActivePodDocument("pod-1")).thenReturn(pod);
    when(releaseService.getActiveReleaseDocument("pod-1", "release-1")).thenReturn(release);
    when(jiraConfigMapper.toJiraFieldConfig(any())).thenReturn(fieldConfig);
    when(jiraClient.searchIssues(any(), eq(fieldConfig), eq(0), eq(25)))
        .thenReturn(searchPage(ticket("CARE-1")));

    IssueSearchPageDto result = issueSearchService.searchInRelease(
        "pod-1", "release-1", new IssueSearchReleaseRequest(null), 0, 25);

    assertThat(result.issues()).hasSize(1);
    verify(jiraClient).searchIssues(
        eq("project = CARE AND fixVersion = \"Q3 2026\""),
        eq(fieldConfig),
        eq(0),
        eq(25));
  }

  @Test
  void searchInReleaseMergesBaseAndAdditionalJqlWithAnd() {
    ReleaseConfigDocument release = releaseWithBaseJql("project = SCRUM AND fixVersion = \"Q3\"");

    PodDocument pod = podWithProjects("pod-1", List.of("SCRUM"));
    JiraFieldConfig fieldConfig = fieldConfig();

    when(teamService.getActivePodDocument("pod-1")).thenReturn(pod);
    when(releaseService.getActiveReleaseDocument("pod-1", "release-1")).thenReturn(release);
    when(jiraConfigMapper.toJiraFieldConfig(any())).thenReturn(fieldConfig);
    when(jiraClient.searchIssues(any(), eq(fieldConfig), eq(0), eq(25)))
        .thenReturn(searchPage(ticket("SCRUM-1")));

    issueSearchService.searchInRelease(
        "pod-1",
        "release-1",
        new IssueSearchReleaseRequest("status = \"In Progress\""),
        0,
        25);

    verify(jiraClient).searchIssues(
        eq("(project = SCRUM AND fixVersion = \"Q3\") AND (status = \"In Progress\")"),
        eq(fieldConfig),
        eq(0),
        eq(25));
  }

  @Test
  void searchInReleaseFallsBackToLegacyFiltersWhenBaseJqlMissing() {
    ReleaseConfigDocument release = new ReleaseConfigDocument();
    release.setId("release-1");
    release.setTeamId("team-1");
    release.setFixVersionIncludes(List.of("Q3 2026"));
    release.setBasicFilters(new ReleaseBasicFilters());

    PodDocument pod = podWithProjects("pod-1", List.of("CARE"));
    JiraFieldConfig fieldConfig = fieldConfig();

    when(teamService.getActivePodDocument("pod-1")).thenReturn(pod);
    when(releaseService.getActiveReleaseDocument("pod-1", "release-1")).thenReturn(release);
    when(jiraConfigMapper.toJiraFieldConfig(any())).thenReturn(fieldConfig);
    when(jiraClient.searchIssues(any(), eq(fieldConfig), eq(0), eq(25)))
        .thenReturn(searchPage(ticket("CARE-1")));

    issueSearchService.searchInRelease(
        "pod-1", "release-1", new IssueSearchReleaseRequest(null), 0, 25);

    verify(jiraClient).searchIssues(
        eq("project IN (\"CARE\") AND fixVersion IN (\"Q3 2026\") ORDER BY updated DESC"),
        eq(fieldConfig),
        eq(0),
        eq(25));
  }

  @Test
  void searchInReleaseThrowsWhenReleaseNotFound() {
    when(teamService.getActivePodDocument("pod-1")).thenReturn(podWithProjects("pod-1", List.of("CARE")));
    when(releaseService.getActiveReleaseDocument("pod-1", "missing"))
        .thenThrow(new ResourceNotFoundException("RELEASE_NOT_FOUND", "Release not found: missing"));

    assertThatThrownBy(() -> issueSearchService.searchInRelease(
        "pod-1", "missing", new IssueSearchReleaseRequest(null), 0, 50))
        .isInstanceOf(ResourceNotFoundException.class);
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

    AnalyticsResponse result = issueSearchService.analyzeRelease(
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

    AnalyticsResponse result = issueSearchService.analyzeRelease(
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

  private IssueSearchPage searchPage(TicketViewDto ticket) {
    return new IssueSearchPage(List.of(ticket), 0, 50, 1, true);
  }

  private TicketViewDto ticket(String key) {
    return new TicketViewDto(
        key, "Summary", "Story", "To Do", StatusCategory.TODO,
        3.0, Domain.DEV, List.of(), null, null, "High", List.of(), List.of(), List.of(), List.of());
  }
}
