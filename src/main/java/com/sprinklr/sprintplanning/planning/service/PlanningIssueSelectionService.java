package com.sprinklr.sprintplanning.planning.service;

import com.sprinklr.sprintplanning.client.jira.JiraClient;
import com.sprinklr.sprintplanning.common.model.IssueView;
import com.sprinklr.sprintplanning.common.model.JiraFieldConfig;
import com.sprinklr.sprintplanning.planning.model.OverrideAction;
import com.sprinklr.sprintplanning.planning.model.PlanningOverride;
import com.sprinklr.sprintplanning.planning.model.SprintPlanningDocument;
import com.sprinklr.sprintplanning.search.dto.TicketViewDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class PlanningIssueSelectionService {

  private final JiraClient jiraClient;
  private final SprintPlanningDocumentAccessor planningDocumentAccessor;

  public PlanningIssueSelectionService(
      JiraClient jiraClient,
      SprintPlanningDocumentAccessor planningDocumentAccessor) {
    this.jiraClient = jiraClient;
    this.planningDocumentAccessor = planningDocumentAccessor;
  }

  public List<IssueView> resolveSelectedIssues(
      SprintPlanningDocument planning,
      List<IssueView> sprintIssues,
      Long boardId,
      JiraFieldConfig fieldConfig) {
    Set<String> excludeKeys = planningDocumentAccessor.overrides(planning).stream()
        .filter(override -> override.getAction() == OverrideAction.EXCLUDE)
        .map(PlanningOverride::getIssueKey)
        .collect(Collectors.toSet());

    Set<String> includeKeys = planningDocumentAccessor.overrides(planning).stream()
        .filter(override -> override.getAction() == OverrideAction.INCLUDE)
        .map(PlanningOverride::getIssueKey)
        .collect(Collectors.toSet());

    Map<String, IssueView> selected = new LinkedHashMap<>();
    for (IssueView issue : sprintIssues) {
      if (!excludeKeys.contains(issue.key())) {
        selected.put(issue.key(), issue);
      }
    }

    if (!includeKeys.isEmpty()) {
      Set<String> missingIncludeKeys = new HashSet<>(includeKeys);
      missingIncludeKeys.removeAll(selected.keySet());
      if (!missingIncludeKeys.isEmpty()) {
        List<IssueView> backlogIssues = jiraClient.getBacklogIssues(boardId, fieldConfig);
        for (IssueView issue : backlogIssues) {
          if (missingIncludeKeys.contains(issue.key())) {
            selected.put(issue.key(), issue);
            missingIncludeKeys.remove(issue.key());
          }
        }
      }
    }

    return new ArrayList<>(selected.values());
  }

  public List<IssueView> fetchCommittedIssues(PodJiraContext podContext, List<String> committedKeys) {
    if (committedKeys == null || committedKeys.isEmpty()) {
      return List.of();
    }

    List<TicketViewDto> tickets = jiraClient.getIssuesByKeys(committedKeys, podContext.fieldConfig());
    return tickets.stream()
        .map(this::toIssueView)
        .toList();
  }

  public IssueView toIssueView(TicketViewDto ticket) {
    return new IssueView(
        ticket.key(),
        ticket.summary(),
        ticket.domain(),
        ticket.storyPoints(),
        ticket.issueType(),
        ticket.status(),
        ticket.statusCategory(),
        ticket.domainAllocations() != null ? ticket.domainAllocations() : List.of(),
        List.of(),
        ticket.domainLabel());
  }
}
