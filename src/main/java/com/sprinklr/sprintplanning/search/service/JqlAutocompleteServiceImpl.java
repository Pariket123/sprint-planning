package com.sprinklr.sprintplanning.search.service;

import com.sprinklr.sprintplanning.client.jira.JiraRestClient;
import com.sprinklr.sprintplanning.client.jira.dto.JiraJqlReferenceDataDto;
import com.sprinklr.sprintplanning.client.jira.dto.JiraJqlSuggestionItemDto;
import com.sprinklr.sprintplanning.client.jira.dto.JiraJqlSuggestionsDto;
import com.sprinklr.sprintplanning.client.jira.dto.JiraJqlVisibleFieldDto;
import com.sprinklr.sprintplanning.client.jira.dto.JiraJqlVisibleFunctionDto;
import com.sprinklr.sprintplanning.client.jira.dto.JiraProjectDto;
import com.sprinklr.sprintplanning.common.exception.JiraClientException;
import com.sprinklr.sprintplanning.search.dto.JqlFieldReferenceDto;
import com.sprinklr.sprintplanning.search.dto.JqlFunctionReferenceDto;
import com.sprinklr.sprintplanning.search.dto.JqlReferenceDataDto;
import com.sprinklr.sprintplanning.search.dto.JqlSuggestionItemDto;
import com.sprinklr.sprintplanning.search.dto.JqlSuggestionsDto;
import com.sprinklr.sprintplanning.team.model.PodDocument;
import com.sprinklr.sprintplanning.team.model.PodJiraConfig;
import com.sprinklr.sprintplanning.team.service.TeamService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class JqlAutocompleteServiceImpl implements JqlAutocompleteService {

  private final TeamService teamService;
  private final JiraRestClient jiraRestClient;

  public JqlAutocompleteServiceImpl(TeamService teamService, JiraRestClient jiraRestClient) {
    this.teamService = teamService;
    this.jiraRestClient = jiraRestClient;
  }

  @Override
  public JqlReferenceDataDto getReferenceData(String podId) {
    PodDocument pod = teamService.getActivePodDocument(podId);
    List<Long> projectIds = resolveProjectIds(pod.getJiraConfig());
    JiraJqlReferenceDataDto referenceData = jiraRestClient.getJqlAutocompleteData(projectIds);
    return toReferenceData(referenceData);
  }

  @Override
  public JqlSuggestionsDto getSuggestions(
      String podId,
      String fieldName,
      String fieldValue,
      String predicateName,
      String predicateValue) {
    teamService.getActivePodDocument(podId);
    if (fieldName == null || fieldName.isBlank()) {
      return JqlSuggestionsDto.empty();
    }

    JiraJqlSuggestionsDto suggestions = jiraRestClient.getJqlAutocompleteSuggestions(
        fieldName.trim(),
        normalizeOptional(fieldValue),
        normalizeOptional(predicateName),
        normalizeOptional(predicateValue));

    if (suggestions == null || suggestions.getResults() == null) {
      return JqlSuggestionsDto.empty();
    }

    List<JqlSuggestionItemDto> results = suggestions.getResults().stream()
        .map(this::toSuggestionItem)
        .toList();
    return new JqlSuggestionsDto(results);
  }

  private List<Long> resolveProjectIds(PodJiraConfig jiraConfig) {
    if (jiraConfig == null || jiraConfig.getProjectKeys() == null) {
      return List.of();
    }

    Set<Long> projectIds = new LinkedHashSet<>();
    for (String projectKey : jiraConfig.getProjectKeys()) {
      if (projectKey == null || projectKey.isBlank()) {
        continue;
      }
      try {
        JiraProjectDto project = jiraRestClient.getProject(projectKey.trim());
        if (project != null && project.getId() != null) {
          projectIds.add(project.getId());
        }
      } catch (JiraClientException ignored) {
        // Skip unknown project keys and fall back to unscoped autocomplete data.
      }
    }
    return new ArrayList<>(projectIds);
  }

  private JqlReferenceDataDto toReferenceData(JiraJqlReferenceDataDto referenceData) {
    if (referenceData == null) {
      return new JqlReferenceDataDto(List.of(), List.of(), List.of());
    }

    List<JqlFieldReferenceDto> fields = referenceData.getVisibleFieldNames().stream()
        .map(this::toFieldReference)
        .toList();
    List<JqlFunctionReferenceDto> functions = referenceData.getVisibleFunctionNames().stream()
        .map(this::toFunctionReference)
        .toList();
    return new JqlReferenceDataDto(
        List.copyOf(referenceData.getJqlReservedWords()),
        fields,
        functions);
  }

  private JqlFieldReferenceDto toFieldReference(JiraJqlVisibleFieldDto field) {
    return new JqlFieldReferenceDto(
        field.getValue(),
        field.getDisplayName(),
        field.getCfid(),
        List.copyOf(field.getOperators()));
  }

  private JqlFunctionReferenceDto toFunctionReference(JiraJqlVisibleFunctionDto function) {
    return new JqlFunctionReferenceDto(function.getValue(), function.getDisplayName());
  }

  private JqlSuggestionItemDto toSuggestionItem(JiraJqlSuggestionItemDto item) {
    return new JqlSuggestionItemDto(item.getValue(), item.getDisplayName());
  }

  private String normalizeOptional(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }
}
