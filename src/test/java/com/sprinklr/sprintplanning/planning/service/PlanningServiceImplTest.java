package com.sprinklr.sprintplanning.planning.service;

import com.sprinklr.sprintplanning.client.jira.JiraClient;
import com.sprinklr.sprintplanning.common.enums.Domain;
import com.sprinklr.sprintplanning.common.enums.StatusCategory;
import com.sprinklr.sprintplanning.common.exception.JiraClientException;
import com.sprinklr.sprintplanning.common.model.IssueView;
import com.sprinklr.sprintplanning.common.model.JiraFieldConfig;
import com.sprinklr.sprintplanning.common.model.SprintView;
import com.sprinklr.sprintplanning.planning.calculator.CapacityAllocationCalculator;
import com.sprinklr.sprintplanning.planning.calculator.PlanningCalculator;
import com.sprinklr.sprintplanning.planning.config.PlanningProperties;
import com.sprinklr.sprintplanning.planning.dto.IssueMoveRequest;
import com.sprinklr.sprintplanning.planning.dto.PlannedIssueViewDto;
import com.sprinklr.sprintplanning.planning.dto.PlannedScopeDto;
import com.sprinklr.sprintplanning.planning.dto.PlanningIssuesPageDto;
import com.sprinklr.sprintplanning.planning.dto.PlanningSummaryDto;
import com.sprinklr.sprintplanning.planning.dto.PlanningViewDto;
import com.sprinklr.sprintplanning.planning.dto.CapacityRiskStatus;
import com.sprinklr.sprintplanning.planning.model.PersonCapacity;
import com.sprinklr.sprintplanning.planning.model.OverrideAction;
import com.sprinklr.sprintplanning.planning.model.PlanningOverride;
import com.sprinklr.sprintplanning.planning.model.SprintPlanningDocument;
import com.sprinklr.sprintplanning.planning.mapper.PlanningMapper;
import com.sprinklr.sprintplanning.planning.repository.SprintPlanningRepository;
import com.sprinklr.sprintplanning.search.dto.TicketViewDto;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
  @Mock
  private RolloverService rolloverService;

  private PlanningService planningService;

  private static final Executor SYNC_EXECUTOR = Runnable::run;

  @BeforeEach
  void setUp() {
    PlanningProperties properties = new PlanningProperties();
    PlanningCalculator calculator = new PlanningCalculator(properties, new CapacityAllocationCalculator());
    PlanningMapper planningMapper = Mappers.getMapper(PlanningMapper.class);
    SprintPlanningDocumentAccessor planningDocumentAccessor =
        new SprintPlanningDocumentAccessor(sprintPlanningRepository);
    planningService = new PlanningServiceImpl(
        teamService, jiraClient, jiraConfigMapper, sprintPlanningRepository,
        planningDocumentAccessor, calculator, planningMapper, rolloverService, SYNC_EXECUTOR);
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

    when(sprintPlanningRepository.findAllByPodIdAndJiraSprintIdOrderByUpdatedAtDesc("pod-1", 20L))
        .thenReturn(List.of(planning));
    when(jiraClient.getSprintIssues(eq(20L), any())).thenReturn(List.of(
        new IssueView("WFM-1", "Excluded", Domain.DEV, 2.0, "Story", "To Do", StatusCategory.TODO),
        new IssueView("WFM-2", "Kept", Domain.QA, 3.0, "Story", "To Do", StatusCategory.TODO)));
    when(jiraClient.getBacklogIssues(eq(101L), any())).thenReturn(List.of(
        new IssueView("WFM-99", "Included", Domain.DEV, 5.0, "Story", "To Do", StatusCategory.TODO)));

    List<IssueView> selected = planningService.resolveSelectedIssues("pod-1", 20L);

    assertThat(selected).extracting(IssueView::key).containsExactlyInAnyOrder("WFM-2", "WFM-99");
  }

  @Test
  void updatePlannedScopeStoresNormalizedIssueKeys() {
    when(teamService.getActivePodDocument("pod-1")).thenReturn(podWithBoard(101L));
    SprintPlanningDocument planning = new SprintPlanningDocument();
    planning.setPodId("pod-1");
    planning.setJiraSprintId(12L);
    when(sprintPlanningRepository.findAllByPodIdAndJiraSprintIdOrderByUpdatedAtDesc("pod-1", 12L))
        .thenReturn(List.of(planning));
    when(sprintPlanningRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    PlannedScopeDto scope = planningService.updatePlannedScope(
        "pod-1", 12L, List.of(" CARE-105613 ", "CARE-105613", "CARE-105614"));

    assertThat(scope.plannedIssueKeys()).containsExactly("CARE-105613", "CARE-105614");
    assertThat(scope.plannedScopeCapturedAt()).isNotNull();
    assertThat(planning.getPlannedIssueKeys()).containsExactly("CARE-105613", "CARE-105614");
  }

  @Test
  void getPlannedIssuesFetchesLatestJiraDetailsByIssueKey() {
    PodDocument pod = podWithBoard(101L);
    when(teamService.getActivePodDocument("pod-1")).thenReturn(pod);
    when(jiraConfigMapper.toJiraFieldConfig(any())).thenReturn(fieldConfig());

    SprintPlanningDocument planning = new SprintPlanningDocument();
    planning.setPodId("pod-1");
    planning.setJiraSprintId(12L);
    planning.setPlannedIssueKeys(new ArrayList<>(List.of("CARE-105613", "CARE-105614")));
    when(sprintPlanningRepository.findAllByPodIdAndJiraSprintIdOrderByUpdatedAtDesc("pod-1", 12L))
        .thenReturn(List.of(planning));

    when(jiraClient.getIssuesByKeys(eq(List.of("CARE-105613", "CARE-105614")), any()))
        .thenReturn(List.of(
            ticket("CARE-105613", List.of(13L)),
            ticket("CARE-105614", List.of(12L))));

    List<PlannedIssueViewDto> plannedIssues = planningService.getPlannedIssues("pod-1", 12L);

    assertThat(plannedIssues).hasSize(2);
    assertThat(plannedIssues.get(0).key()).isEqualTo("CARE-105613");
    assertThat(plannedIssues.get(0).plannedSprintId()).isEqualTo(12L);
    assertThat(plannedIssues.get(0).currentSprintId()).isEqualTo(13L);
    assertThat(plannedIssues.get(0).rolledOver()).isTrue();
    assertThat(plannedIssues.get(1).rolledOver()).isFalse();
    verify(jiraClient).getIssuesByKeys(eq(List.of("CARE-105613", "CARE-105614")), any());
  }

  @Test
  void getPlannedIssuesDoesNotMarkBacklogIssuesAsRolledOverWhenNeverCommitted() {
    PodDocument pod = podWithBoard(101L);
    when(teamService.getActivePodDocument("pod-1")).thenReturn(pod);
    when(jiraConfigMapper.toJiraFieldConfig(any())).thenReturn(fieldConfig());

    SprintPlanningDocument planning = new SprintPlanningDocument();
    planning.setPodId("pod-1");
    planning.setJiraSprintId(12L);
    planning.setPlannedIssueKeys(new ArrayList<>(List.of("CARE-105615")));
    when(sprintPlanningRepository.findAllByPodIdAndJiraSprintIdOrderByUpdatedAtDesc("pod-1", 12L))
        .thenReturn(List.of(planning));

    when(jiraClient.getIssuesByKeys(eq(List.of("CARE-105615")), any()))
        .thenReturn(List.of(ticket("CARE-105615", List.of())));

    List<PlannedIssueViewDto> plannedIssues = planningService.getPlannedIssues("pod-1", 12L);

    assertThat(plannedIssues).hasSize(1);
    assertThat(plannedIssues.getFirst().currentSprintId()).isNull();
    assertThat(plannedIssues.getFirst().rolledOver()).isFalse();
  }

  @Test
  void getPlannedIssuesMarksCommittedBacklogIssuesAsRolledOver() {
    PodDocument pod = podWithBoard(101L);
    when(teamService.getActivePodDocument("pod-1")).thenReturn(pod);
    when(jiraConfigMapper.toJiraFieldConfig(any())).thenReturn(fieldConfig());

    SprintPlanningDocument planning = new SprintPlanningDocument();
    planning.setPodId("pod-1");
    planning.setJiraSprintId(12L);
    planning.setPlannedIssueKeys(new ArrayList<>(List.of("CARE-105615")));
    planning.setCommittedIssueKeys(new ArrayList<>(List.of("CARE-105615")));
    when(sprintPlanningRepository.findAllByPodIdAndJiraSprintIdOrderByUpdatedAtDesc("pod-1", 12L))
        .thenReturn(List.of(planning));

    when(jiraClient.getIssuesByKeys(eq(List.of("CARE-105615")), any()))
        .thenReturn(List.of(ticket("CARE-105615", List.of())));

    List<PlannedIssueViewDto> plannedIssues = planningService.getPlannedIssues("pod-1", 12L);

    assertThat(plannedIssues).hasSize(1);
    assertThat(plannedIssues.getFirst().rolledOver()).isTrue();
  }

  @Test
  void moveIssuesToSprintUpdatesCommittedIssueKeysAfterJiraSuccess() {
    PodDocument pod = podWithBoard(101L);
    when(teamService.getActivePodDocument("pod-1")).thenReturn(pod);
    when(jiraConfigMapper.toJiraFieldConfig(any())).thenReturn(fieldConfig());

    SprintPlanningDocument planning = new SprintPlanningDocument();
    planning.setPodId("pod-1");
    planning.setJiraSprintId(20L);
    planning.setCommittedIssueKeys(new ArrayList<>(List.of("WFM-0")));
    when(sprintPlanningRepository.findAllByPodIdAndJiraSprintIdOrderByUpdatedAtDesc("pod-1", 20L))
        .thenReturn(List.of(planning));
    when(sprintPlanningRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    SprintView sprint = new SprintView(20L, "Sprint 20", "active", Instant.now(), Instant.now(), null);
    when(jiraClient.getSprint(20L)).thenReturn(sprint);
    when(jiraClient.getBoardSprints(101L, "closed")).thenReturn(List.of());
    when(jiraClient.getSprintIssues(eq(20L), any())).thenReturn(List.of());
    when(rolloverService.getRolloverRecords("pod-1", 20L)).thenReturn(List.of());

    planningService.moveIssuesToSprint(
        "pod-1", 20L, new IssueMoveRequest(List.of("WFM-1", "WFM-0"), false));

    verify(jiraClient).moveIssuesToSprint(List.of("WFM-1", "WFM-0"), 20L);
    assertThat(planning.getCommittedIssueKeys()).containsExactly("WFM-0", "WFM-1");
    assertThat(planning.getCommittedAt()).isNotNull();
  }

  @Test
  void moveIssuesToSprintAddsToPlannedScopeWhenRequested() {
    PodDocument pod = podWithBoard(101L);
    when(teamService.getActivePodDocument("pod-1")).thenReturn(pod);
    when(jiraConfigMapper.toJiraFieldConfig(any())).thenReturn(fieldConfig());

    SprintPlanningDocument planning = new SprintPlanningDocument();
    planning.setPodId("pod-1");
    planning.setJiraSprintId(20L);
    when(sprintPlanningRepository.findAllByPodIdAndJiraSprintIdOrderByUpdatedAtDesc("pod-1", 20L))
        .thenReturn(List.of(planning));
    when(sprintPlanningRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    SprintView sprint = new SprintView(20L, "Sprint 20", "active", Instant.now(), Instant.now(), null);
    when(jiraClient.getSprint(20L)).thenReturn(sprint);
    when(jiraClient.getBoardSprints(101L, "closed")).thenReturn(List.of());
    when(jiraClient.getSprintIssues(eq(20L), any())).thenReturn(List.of());
    when(rolloverService.getRolloverRecords("pod-1", 20L)).thenReturn(List.of());

    planningService.moveIssuesToSprint(
        "pod-1", 20L, new IssueMoveRequest(List.of("WFM-1"), true));

    assertThat(planning.getPlannedIssueKeys()).containsExactly("WFM-1");
    assertThat(planning.getPlannedScopeCapturedAt()).isNotNull();
  }

  @Test
  void moveIssuesToSprintDoesNotUpdateCommittedKeysWhenJiraFails() {
    when(teamService.getActivePodDocument("pod-1")).thenReturn(podWithBoard(101L));

    doThrow(JiraClientException.badRequest("move failed"))
        .when(jiraClient).moveIssuesToSprint(any(), eq(20L));

    assertThatThrownBy(() -> planningService.moveIssuesToSprint(
        "pod-1", 20L, new IssueMoveRequest(List.of("WFM-1"), false)))
        .isInstanceOf(JiraClientException.class);

    verify(sprintPlanningRepository, never()).save(any());
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

  @Test
  void calculateSummaryFetchesCommittedIssuesFromJira() {
    PodDocument pod = podWithBoard(101L);
    when(teamService.getActivePodDocument("pod-1")).thenReturn(pod);
    when(jiraConfigMapper.toJiraFieldConfig(any())).thenReturn(fieldConfig());

    SprintPlanningDocument planning = new SprintPlanningDocument();
    planning.setPodId("pod-1");
    planning.setJiraSprintId(20L);
    planning.setCommittedIssueKeys(new ArrayList<>(List.of("WFM-9")));
    PersonCapacity devCapacity = new PersonCapacity();
    devCapacity.setPersonName("Dev");
    devCapacity.setDomain(Domain.DEV);
    devCapacity.setBandwidthPercent(100);
    planning.setCapacity(List.of(devCapacity));

    when(sprintPlanningRepository.findAllByPodIdAndJiraSprintIdOrderByUpdatedAtDesc("pod-1", 20L))
        .thenReturn(List.of(planning));

    SprintView sprint = new SprintView(
        20L, "Sprint 20", "active",
        Instant.parse("2026-06-08T00:00:00Z"),
        Instant.parse("2026-06-12T00:00:00Z"),
        null);
    when(jiraClient.getSprint(20L)).thenReturn(sprint);
    when(jiraClient.getBoardSprints(101L, "closed")).thenReturn(List.of());
    when(jiraClient.getSprintIssues(eq(20L), any())).thenReturn(List.of());
    when(jiraClient.getIssuesByKeys(eq(List.of("WFM-9")), any()))
        .thenReturn(List.of(ticket("WFM-9", List.of(20L), Domain.BE, 6.0)));

    PlanningSummaryDto summary = planningService.calculateSummary("pod-1", 20L);

    verify(jiraClient).getIssuesByKeys(eq(List.of("WFM-9")), any());
    assertThat(summary.domainMetrics()).filteredOn(m -> m.domain() == Domain.BE).first()
        .satisfies(be -> {
          assertThat(be.committedStoryPoints()).isEqualTo(6.0);
          assertThat(be.capacityRisk()).isEqualTo(CapacityRiskStatus.OVER_CAPACITY);
        });
  }

  @Test
  void getPlanningViewIncludesDomainMetrics() {
    PodDocument pod = podWithBoard(101L);
    when(teamService.getActivePodDocument("pod-1")).thenReturn(pod);
    when(jiraConfigMapper.toJiraFieldConfig(any())).thenReturn(fieldConfig());

    SprintPlanningDocument planning = new SprintPlanningDocument();
    planning.setPodId("pod-1");
    planning.setJiraSprintId(20L);
    when(sprintPlanningRepository.findAllByPodIdAndJiraSprintIdOrderByUpdatedAtDesc("pod-1", 20L))
        .thenReturn(List.of(planning));

    SprintView sprint = new SprintView(
        20L, "Sprint 20", "active",
        Instant.parse("2026-06-08T00:00:00Z"),
        Instant.parse("2026-06-12T00:00:00Z"),
        null);
    when(jiraClient.getSprint(20L)).thenReturn(sprint);
    when(jiraClient.getBoardSprints(101L, "closed")).thenReturn(List.of());
    when(jiraClient.getSprintIssues(eq(20L), any())).thenReturn(List.of());
    when(rolloverService.getRolloverRecords("pod-1", 20L)).thenReturn(List.of());

    PlanningViewDto view = planningService.getPlanningView("pod-1", 20L);

    assertThat(view.domainMetrics()).isNotNull();
    assertThat(view.domainMetrics()).isNotEmpty();
    assertThat(view.sprintIssueCount()).isZero();
    assertThat(view.selectedIssueCount()).isZero();
    assertThat(view.selectedIssueKeys()).isEmpty();
  }

  @Test
  void getPlanningViewFetchesEachJiraResourceOnce() {
    PodDocument pod = podWithBoard(101L);
    when(teamService.getActivePodDocument("pod-1")).thenReturn(pod);
    when(jiraConfigMapper.toJiraFieldConfig(any())).thenReturn(fieldConfig());

    SprintPlanningDocument planning = new SprintPlanningDocument();
    planning.setPodId("pod-1");
    planning.setJiraSprintId(20L);
    when(sprintPlanningRepository.findAllByPodIdAndJiraSprintIdOrderByUpdatedAtDesc("pod-1", 20L))
        .thenReturn(List.of(planning));

    SprintView sprint = new SprintView(
        20L, "Sprint 20", "active",
        Instant.parse("2026-06-08T00:00:00Z"),
        Instant.parse("2026-06-12T00:00:00Z"),
        null);
    when(jiraClient.getSprint(20L)).thenReturn(sprint);
    when(jiraClient.getBoardSprints(101L, "closed")).thenReturn(List.of());
    when(jiraClient.getSprintIssues(eq(20L), any())).thenReturn(List.of());
    when(rolloverService.getRolloverRecords("pod-1", 20L)).thenReturn(List.of());

    planningService.getPlanningView("pod-1", 20L);

    verify(jiraClient, times(1)).getSprint(20L);
    verify(jiraClient, times(1)).getSprintIssues(eq(20L), any());
    verify(jiraClient, times(1)).getBoardSprints(101L, "closed");
  }

  @Test
  void getPlanningIssuesReturnsPaginatedSprintIssues() {
    PodDocument pod = podWithBoard(101L);
    when(teamService.getActivePodDocument("pod-1")).thenReturn(pod);
    when(jiraConfigMapper.toJiraFieldConfig(any())).thenReturn(fieldConfig());

    SprintPlanningDocument planning = new SprintPlanningDocument();
    planning.setPodId("pod-1");
    planning.setJiraSprintId(20L);
    when(sprintPlanningRepository.findAllByPodIdAndJiraSprintIdOrderByUpdatedAtDesc("pod-1", 20L))
        .thenReturn(List.of(planning));

    List<IssueView> sprintIssues = List.of(
        issue("WFM-1"), issue("WFM-2"), issue("WFM-3"));
    when(jiraClient.getSprintIssues(eq(20L), any())).thenReturn(sprintIssues);

    PlanningIssuesPageDto page = planningService.getPlanningIssues("pod-1", 20L, 1, 2);

    assertThat(page.sprintIssues()).containsExactly(issue("WFM-2"), issue("WFM-3"));
    assertThat(page.selectedIssues()).hasSize(3);
    assertThat(page.sprintIssueTotal()).isEqualTo(3);
    assertThat(page.startAt()).isEqualTo(1);
    assertThat(page.maxResults()).isEqualTo(2);
    assertThat(page.last()).isTrue();
  }

  private static IssueView issue(String key) {
    return new IssueView(key, "Summary", Domain.DEV, 2.0, "Story", "Open", StatusCategory.TODO);
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
    return new JiraFieldConfig("sp", "domain", "customfield_10020", Map.of(), List.of("Bug"), List.of("Story"));
  }

  private static TicketViewDto ticket(String key, List<Long> sprintIds) {
    return ticket(key, sprintIds, Domain.DEV, 2.0);
  }

  private static TicketViewDto ticket(String key, List<Long> sprintIds, Domain domain, double storyPoints) {
    return new TicketViewDto(
        key, "Summary", "Story", "In Progress", StatusCategory.IN_PROGRESS,
        storyPoints, domain, List.of(), null, null, "High", List.of(), sprintIds, List.of(), List.of());
  }
}
