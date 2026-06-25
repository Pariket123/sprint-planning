package com.sprinklr.sprintplanning.planning.service;

import com.sprinklr.sprintplanning.client.jira.JiraClient;
import com.sprinklr.sprintplanning.common.model.JiraFieldConfig;
import com.sprinklr.sprintplanning.common.util.StringListNormalizer;
import com.sprinklr.sprintplanning.planning.dto.RecordRolloverRequest;
import com.sprinklr.sprintplanning.planning.dto.RolloverIssueDto;
import com.sprinklr.sprintplanning.planning.mapper.RolloverMapper;
import com.sprinklr.sprintplanning.planning.model.RolloverIssue;
import com.sprinklr.sprintplanning.planning.model.SprintPlanningDocument;
import com.sprinklr.sprintplanning.planning.repository.SprintPlanningRepository;
import com.sprinklr.sprintplanning.search.dto.TicketViewDto;
import com.sprinklr.sprintplanning.team.mapper.JiraConfigMapper;
import com.sprinklr.sprintplanning.team.model.PodDocument;
import com.sprinklr.sprintplanning.team.service.TeamService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Service
public class RolloverServiceImpl implements RolloverService {

  private final TeamService teamService;
  private final JiraClient jiraClient;
  private final JiraConfigMapper jiraConfigMapper;
  private final SprintPlanningRepository sprintPlanningRepository;
  private final SprintPlanningDocumentAccessor planningDocumentAccessor;
  private final RolloverMapper rolloverMapper;

  public RolloverServiceImpl(
      TeamService teamService,
      JiraClient jiraClient,
      JiraConfigMapper jiraConfigMapper,
      SprintPlanningRepository sprintPlanningRepository,
      SprintPlanningDocumentAccessor planningDocumentAccessor,
      RolloverMapper rolloverMapper) {
    this.teamService = teamService;
    this.jiraClient = jiraClient;
    this.jiraConfigMapper = jiraConfigMapper;
    this.sprintPlanningRepository = sprintPlanningRepository;
    this.planningDocumentAccessor = planningDocumentAccessor;
    this.rolloverMapper = rolloverMapper;
  }

  @Override
  public RolloverIssueDto recordRollover(String podId, Long fromSprintId, RecordRolloverRequest request) {
    PodDocument pod = teamService.getActivePodDocument(podId);
    JiraFieldConfig fieldConfig = jiraConfigMapper.toJiraFieldConfig(pod.getJiraConfig());
    String issueKey = StringListNormalizer.normalize(List.of(request.issueKey())).getFirst();

    List<TicketViewDto> tickets = jiraClient.getIssuesByKeys(List.of(issueKey), fieldConfig);
    TicketViewDto ticket = tickets.isEmpty() ? null : tickets.getFirst();

    if (request.shouldMoveInJira()) {
      jiraClient.moveIssuesToSprint(List.of(issueKey), request.toSprintId());
      updateCommittedIssueKeys(podId, request.toSprintId(), List.of(issueKey));
    }

    SprintPlanningDocument fromPlanning = planningDocumentAccessor.getOrCreate(podId, fromSprintId);
    RolloverIssue rolloverIssue = buildRolloverIssue(
        issueKey, fromSprintId, request.toSprintId(), ticket, request.notes());
    planningDocumentAccessor.rolloverIssues(fromPlanning).add(rolloverIssue);
    save(fromPlanning);

    return rolloverMapper.toDto(rolloverIssue);
  }

  @Override
  public List<RolloverIssueDto> getRolloverRecords(String podId, Long jiraSprintId) {
    teamService.getActivePodDocument(podId);
    Map<String, RolloverIssueDto> merged = new LinkedHashMap<>();
    for (RolloverIssueDto outgoing : getOutgoingRollovers(podId, jiraSprintId)) {
      merged.put(outgoing.issueKey() + ":" + outgoing.fromSprintId() + ":" + outgoing.toSprintId(), outgoing);
    }
    for (RolloverIssueDto incoming : getIncomingRollovers(podId, jiraSprintId)) {
      merged.put(incoming.issueKey() + ":" + incoming.fromSprintId() + ":" + incoming.toSprintId(), incoming);
    }
    return new ArrayList<>(merged.values());
  }

  @Override
  public List<RolloverIssueDto> getOutgoingRollovers(String podId, Long jiraSprintId) {
    teamService.getActivePodDocument(podId);
    SprintPlanningDocument planning = planningDocumentAccessor.getOrCreate(podId, jiraSprintId);
    return planningDocumentAccessor.rolloverIssues(planning).stream()
        .filter(issue -> jiraSprintId.equals(issue.getFromSprintId()))
        .map(rolloverMapper::toDto)
        .toList();
  }

  @Override
  public List<RolloverIssueDto> getIncomingRollovers(String podId, Long jiraSprintId) {
    teamService.getActivePodDocument(podId);
    return sprintPlanningRepository.findIncomingRollovers(podId, jiraSprintId).stream()
        .flatMap(document -> planningDocumentAccessor.rolloverIssues(document).stream())
        .filter(issue -> jiraSprintId.equals(issue.getToSprintId()))
        .map(rolloverMapper::toDto)
        .toList();
  }

  private RolloverIssue buildRolloverIssue(
      String issueKey,
      Long fromSprintId,
      Long toSprintId,
      TicketViewDto ticket,
      String notes) {
    RolloverIssue rolloverIssue = new RolloverIssue();
    rolloverIssue.setIssueKey(issueKey);
    rolloverIssue.setFromSprintId(fromSprintId);
    rolloverIssue.setToSprintId(toSprintId);
    rolloverIssue.setStatusAtRollover(ticket != null ? ticket.status() : null);
    rolloverIssue.setStoryPointsAtRollover(ticket != null ? ticket.storyPoints() : null);
    rolloverIssue.setDomain(ticket != null ? ticket.domain() : null);
    rolloverIssue.setRolledOverAt(Instant.now());
    rolloverIssue.setRolledOverBy(resolveCurrentUserId());
    rolloverIssue.setNotes(notes != null ? notes.trim() : null);
    return rolloverIssue;
  }

  private void updateCommittedIssueKeys(String podId, Long jiraSprintId, List<String> issueKeys) {
    SprintPlanningDocument planning = planningDocumentAccessor.getOrCreate(podId, jiraSprintId);
    LinkedHashSet<String> merged = new LinkedHashSet<>(planningDocumentAccessor.normalize(planning).getCommittedIssueKeys());
    merged.addAll(issueKeys);
    planning.setCommittedIssueKeys(new ArrayList<>(merged));
    planning.setCommittedAt(Instant.now());
    save(planning);
  }

  private String resolveCurrentUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
      return jwtAuthentication.getToken().getClaimAsString("oid");
    }
    return null;
  }

  private SprintPlanningDocument save(SprintPlanningDocument document) {
    return planningDocumentAccessor.save(document);
  }
}
