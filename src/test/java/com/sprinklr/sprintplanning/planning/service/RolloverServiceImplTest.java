package com.sprinklr.sprintplanning.planning.service;

import com.sprinklr.sprintplanning.client.jira.JiraClient;
import com.sprinklr.sprintplanning.common.enums.Domain;
import com.sprinklr.sprintplanning.common.enums.StatusCategory;
import com.sprinklr.sprintplanning.common.exception.JiraClientException;
import com.sprinklr.sprintplanning.common.model.JiraFieldConfig;
import com.sprinklr.sprintplanning.planning.dto.RecordRolloverRequest;
import com.sprinklr.sprintplanning.planning.dto.RolloverIssueDto;
import com.sprinklr.sprintplanning.planning.mapper.RolloverMapper;
import com.sprinklr.sprintplanning.planning.model.RolloverIssue;
import com.sprinklr.sprintplanning.planning.model.SprintPlanningDocument;
import com.sprinklr.sprintplanning.planning.repository.SprintPlanningRepository;
import com.sprinklr.sprintplanning.search.dto.TicketViewDto;
import com.sprinklr.sprintplanning.team.mapper.JiraConfigMapper;
import com.sprinklr.sprintplanning.team.model.PodDocument;
import com.sprinklr.sprintplanning.team.model.PodJiraConfig;
import com.sprinklr.sprintplanning.team.service.TeamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RolloverServiceImplTest {

  @Mock
  private TeamService teamService;
  @Mock
  private JiraClient jiraClient;
  @Mock
  private JiraConfigMapper jiraConfigMapper;
  @Mock
  private SprintPlanningRepository sprintPlanningRepository;

  private RolloverService rolloverService;

  @BeforeEach
  void setUp() {
    rolloverService = new RolloverServiceImpl(
        teamService, jiraClient, jiraConfigMapper, sprintPlanningRepository, new RolloverMapper());
  }

  @Test
  void recordRolloverWithoutMoveInJiraOnlyPersistsMongoRecord() {
    when(teamService.getActivePodDocument("pod-1")).thenReturn(pod());
    when(jiraConfigMapper.toJiraFieldConfig(any())).thenReturn(fieldConfig());
    when(jiraClient.getIssuesByKeys(eq(List.of("CARE-105613")), any()))
        .thenReturn(List.of(ticket("CARE-105613", 2.0, Domain.DEV)));

    SprintPlanningDocument fromPlanning = planningDoc("pod-1", 12L);
    when(sprintPlanningRepository.findByPodIdAndJiraSprintId("pod-1", 12L))
        .thenReturn(Optional.of(fromPlanning));
    when(sprintPlanningRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    RolloverIssueDto result = rolloverService.recordRollover(
        "pod-1", 12L, new RecordRolloverRequest("CARE-105613", 13L, "Incomplete", false));

    assertThat(result.issueKey()).isEqualTo("CARE-105613");
    assertThat(result.fromSprintId()).isEqualTo(12L);
    assertThat(result.toSprintId()).isEqualTo(13L);
    assertThat(result.statusAtRollover()).isEqualTo("In Progress");
    assertThat(result.storyPointsAtRollover()).isEqualTo(2.0);
    assertThat(result.domain()).isEqualTo(Domain.DEV);
    assertThat(fromPlanning.getRolloverIssues()).hasSize(1);

    verify(jiraClient, never()).moveIssuesToSprint(any(), any());
    verify(sprintPlanningRepository, never()).findByPodIdAndJiraSprintId("pod-1", 13L);
  }

  @Test
  void recordRolloverWithMoveInJiraMovesIssueAndUpdatesCommittedKeys() {
    when(teamService.getActivePodDocument("pod-1")).thenReturn(pod());
    when(jiraConfigMapper.toJiraFieldConfig(any())).thenReturn(fieldConfig());
    when(jiraClient.getIssuesByKeys(eq(List.of("CARE-105613")), any()))
        .thenReturn(List.of(ticket("CARE-105613", 2.0, Domain.DEV)));

    SprintPlanningDocument fromPlanning = planningDoc("pod-1", 12L);
    SprintPlanningDocument toPlanning = planningDoc("pod-1", 13L);
    when(sprintPlanningRepository.findByPodIdAndJiraSprintId("pod-1", 12L))
        .thenReturn(Optional.of(fromPlanning));
    when(sprintPlanningRepository.findByPodIdAndJiraSprintId("pod-1", 13L))
        .thenReturn(Optional.of(toPlanning));
    when(sprintPlanningRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    rolloverService.recordRollover(
        "pod-1", 12L, new RecordRolloverRequest("CARE-105613", 13L, null, true));

    verify(jiraClient).moveIssuesToSprint(List.of("CARE-105613"), 13L);
    assertThat(toPlanning.getCommittedIssueKeys()).contains("CARE-105613");
    assertThat(toPlanning.getCommittedAt()).isNotNull();
  }

  @Test
  void recordRolloverDoesNotPersistWhenJiraMoveFails() {
    when(teamService.getActivePodDocument("pod-1")).thenReturn(pod());
    when(jiraConfigMapper.toJiraFieldConfig(any())).thenReturn(fieldConfig());
    when(jiraClient.getIssuesByKeys(eq(List.of("CARE-105613")), any()))
        .thenReturn(List.of(ticket("CARE-105613", 2.0, Domain.DEV)));

    doThrow(JiraClientException.badRequest("move failed"))
        .when(jiraClient).moveIssuesToSprint(any(), eq(13L));

    assertThatThrownBy(() -> rolloverService.recordRollover(
        "pod-1", 12L, new RecordRolloverRequest("CARE-105613", 13L, null, true)))
        .isInstanceOf(JiraClientException.class);

    verify(sprintPlanningRepository, never()).save(any());
  }

  @Test
  void getOutgoingRolloversReturnsRecordsFromCurrentSprintDocument() {
    when(teamService.getActivePodDocument("pod-1")).thenReturn(pod());

    SprintPlanningDocument planning = planningDoc("pod-1", 12L);
    RolloverIssue outgoing = new RolloverIssue();
    outgoing.setIssueKey("CARE-1");
    outgoing.setFromSprintId(12L);
    outgoing.setToSprintId(13L);
    planning.getRolloverIssues().add(outgoing);

    when(sprintPlanningRepository.findByPodIdAndJiraSprintId("pod-1", 12L))
        .thenReturn(Optional.of(planning));

    List<RolloverIssueDto> outgoingRollovers = rolloverService.getOutgoingRollovers("pod-1", 12L);

    assertThat(outgoingRollovers).hasSize(1);
    assertThat(outgoingRollovers.get(0).issueKey()).isEqualTo("CARE-1");
  }

  @Test
  void getIncomingRolloversReturnsRecordsFromOtherSprintDocuments() {
    when(teamService.getActivePodDocument("pod-1")).thenReturn(pod());

    SprintPlanningDocument fromPlanning = planningDoc("pod-1", 12L);
    RolloverIssue incoming = new RolloverIssue();
    incoming.setIssueKey("CARE-2");
    incoming.setFromSprintId(12L);
    incoming.setToSprintId(13L);
    fromPlanning.getRolloverIssues().add(incoming);

    when(sprintPlanningRepository.findIncomingRollovers("pod-1", 13L))
        .thenReturn(List.of(fromPlanning));

    List<RolloverIssueDto> incomingRollovers = rolloverService.getIncomingRollovers("pod-1", 13L);

    assertThat(incomingRollovers).hasSize(1);
    assertThat(incomingRollovers.get(0).toSprintId()).isEqualTo(13L);
  }

  private PodDocument pod() {
    PodDocument pod = new PodDocument();
    pod.setId("pod-1");
    pod.setJiraConfig(new PodJiraConfig());
    return pod;
  }

  private JiraFieldConfig fieldConfig() {
    return new JiraFieldConfig("sp", "domain", Map.of(), List.of("Bug"), List.of("Story"));
  }

  private SprintPlanningDocument planningDoc(String podId, Long sprintId) {
    SprintPlanningDocument document = new SprintPlanningDocument();
    document.setPodId(podId);
    document.setJiraSprintId(sprintId);
    document.setRolloverIssues(new ArrayList<>());
    return document;
  }

  private TicketViewDto ticket(String key, double storyPoints, Domain domain) {
    return new TicketViewDto(
        key, "Summary", "Story", "In Progress", StatusCategory.IN_PROGRESS,
        storyPoints, domain, null, null, "High", List.of(), List.of(), List.of(), List.of());
  }
}
