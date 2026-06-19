package com.sprinklr.sprintplanning.planning.service;

import com.sprinklr.sprintplanning.client.jira.JiraClient;
import com.sprinklr.sprintplanning.common.enums.Domain;
import com.sprinklr.sprintplanning.common.enums.StatusCategory;
import com.sprinklr.sprintplanning.common.model.IssueView;
import com.sprinklr.sprintplanning.common.model.JiraFieldConfig;
import com.sprinklr.sprintplanning.common.model.SprintView;
import com.sprinklr.sprintplanning.planning.calculator.PlanningCalculator;
import com.sprinklr.sprintplanning.planning.config.PlanningProperties;
import com.sprinklr.sprintplanning.planning.model.OverrideAction;
import com.sprinklr.sprintplanning.planning.model.PlanningOverride;
import com.sprinklr.sprintplanning.planning.model.SprintPlanningDocument;
import com.sprinklr.sprintplanning.planning.mapper.PlanningMapper;
import com.sprinklr.sprintplanning.planning.repository.SprintPlanningRepository;
import com.sprinklr.sprintplanning.team.mapper.JiraConfigMapper;
import com.sprinklr.sprintplanning.team.model.PodDocument;
import com.sprinklr.sprintplanning.team.model.PodJiraConfig;
import com.sprinklr.sprintplanning.team.service.TeamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanningServiceImplTest {

  @Mock
  private TeamService teamService;
  @Mock
  private JiraClient jiraClient;
  @Mock
  private JiraConfigMapper jiraConfigMapper;
  @Mock
  private SprintPlanningRepository sprintPlanningRepository;

  private PlanningService planningService;

  @BeforeEach
  void setUp() {
    PlanningProperties properties = new PlanningProperties();
    PlanningCalculator calculator = new PlanningCalculator(properties);
    PlanningMapper planningMapper = Mappers.getMapper(PlanningMapper.class);
    planningService = new PlanningServiceImpl(
        teamService, jiraClient, jiraConfigMapper, sprintPlanningRepository, calculator, planningMapper);
  }

  @Test
  void resolveSelectedIssuesAppliesExcludeAndIncludeOverrides() {
    PodDocument pod = podWithBoard(101L);
    when(teamService.getActivePodDocument("pod-1")).thenReturn(pod);
    when(jiraConfigMapper.toJiraFieldConfig(any())).thenReturn(fieldConfig());

    SprintPlanningDocument planning = new SprintPlanningDocument();
    planning.setPodId("pod-1");
    planning.setJiraSprintId(20L);
    PlanningOverride exclude = new PlanningOverride();
    exclude.setIssueKey("WFM-1");
    exclude.setAction(OverrideAction.EXCLUDE);
    PlanningOverride include = new PlanningOverride();
    include.setIssueKey("WFM-99");
    include.setAction(OverrideAction.INCLUDE);
    planning.setOverrides(List.of(exclude, include));

    when(sprintPlanningRepository.findByPodIdAndJiraSprintId("pod-1", 20L))
        .thenReturn(Optional.of(planning));
    when(jiraClient.getSprintIssues(eq(20L), any())).thenReturn(List.of(
        new IssueView("WFM-1", "Excluded", Domain.DEV, 2.0, "Story", "To Do", StatusCategory.TODO),
        new IssueView("WFM-2", "Kept", Domain.QA, 3.0, "Story", "To Do", StatusCategory.TODO)));
    when(jiraClient.getBacklogIssues(eq(101L), any())).thenReturn(List.of(
        new IssueView("WFM-99", "Included", Domain.DEV, 5.0, "Story", "To Do", StatusCategory.TODO)));

    List<IssueView> selected = planningService.resolveSelectedIssues("pod-1", 20L);

    assertThat(selected).extracting(IssueView::key).containsExactlyInAnyOrder("WFM-2", "WFM-99");
  }

  @Test
  void moveIssuesToSprintDelegatesToJiraAndReturnsPlanningView() {
    PodDocument pod = podWithBoard(101L);
    when(teamService.getActivePodDocument("pod-1")).thenReturn(pod);
    when(jiraConfigMapper.toJiraFieldConfig(any())).thenReturn(fieldConfig());
    when(sprintPlanningRepository.findByPodIdAndJiraSprintId("pod-1", 20L))
        .thenReturn(Optional.of(new SprintPlanningDocument()));

    SprintView sprint = new SprintView(20L, "Sprint 20", "active", Instant.now(), Instant.now(), null);
    when(jiraClient.getSprint(20L)).thenReturn(sprint);
    when(jiraClient.getBoardSprints(101L, "closed")).thenReturn(List.of());
    when(jiraClient.getSprintIssues(eq(20L), any())).thenReturn(List.of());

    planningService.moveIssuesToSprint("pod-1", 20L, List.of("WFM-1"));

    verify(jiraClient).moveIssuesToSprint(List.of("WFM-1"), 20L);
  }

  @Test
  void moveIssuesToBacklogDelegatesToJiraAndRefreshesBacklog() {
    PodDocument pod = podWithBoard(101L);
    when(teamService.getActivePodDocument("pod-1")).thenReturn(pod);
    when(jiraConfigMapper.toJiraFieldConfig(any())).thenReturn(fieldConfig());
    when(jiraClient.getBacklogIssues(eq(101L), any(), eq(0), eq(50)))
        .thenReturn(new com.sprinklr.sprintplanning.common.model.BacklogPage(List.of(), 0, 50, 0, true));

    planningService.moveIssuesToBacklog("pod-1", 0, 50, List.of("WFM-3"));

    verify(jiraClient).moveIssuesToBacklog(List.of("WFM-3"));
    verify(jiraClient).getBacklogIssues(101L, fieldConfig(), 0, 50);
  }

  private static PodDocument podWithBoard(Long boardId) {
    PodDocument pod = new PodDocument();
    pod.setId("pod-1");
    PodJiraConfig jiraConfig = new PodJiraConfig();
    jiraConfig.setBoardId(boardId);
    pod.setJiraConfig(jiraConfig);
    return pod;
  }

  private static JiraFieldConfig fieldConfig() {
    return new JiraFieldConfig("sp", "domain", Map.of(), List.of("Bug"), List.of("Story"));
  }
}
