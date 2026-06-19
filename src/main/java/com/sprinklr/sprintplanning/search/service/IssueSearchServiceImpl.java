package com.sprinklr.sprintplanning.search.service;

import com.sprinklr.sprintplanning.client.jira.JiraClient;
import com.sprinklr.sprintplanning.common.exception.ApiException;
import com.sprinklr.sprintplanning.common.model.IssueSearchPage;
import com.sprinklr.sprintplanning.common.model.JiraFieldConfig;
import com.sprinklr.sprintplanning.release.service.ReleaseService;
import com.sprinklr.sprintplanning.search.FilterMergeHelper;
import com.sprinklr.sprintplanning.search.dto.IssueSearchFilters;
import com.sprinklr.sprintplanning.search.dto.IssueSearchPageDto;
import com.sprinklr.sprintplanning.search.jql.JqlBuilder;
import com.sprinklr.sprintplanning.team.mapper.JiraConfigMapper;
import com.sprinklr.sprintplanning.team.model.PodDocument;
import com.sprinklr.sprintplanning.team.service.TeamService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class IssueSearchServiceImpl implements IssueSearchService {

  private final TeamService teamService;
  private final ReleaseService releaseService;
  private final JiraClient jiraClient;
  private final JiraConfigMapper jiraConfigMapper;
  private final JqlBuilder jqlBuilder;
  private final FilterMergeHelper filterMergeHelper;

  public IssueSearchServiceImpl(
      TeamService teamService,
      ReleaseService releaseService,
      JiraClient jiraClient,
      JiraConfigMapper jiraConfigMapper,
      JqlBuilder jqlBuilder,
      FilterMergeHelper filterMergeHelper) {
    this.teamService = teamService;
    this.releaseService = releaseService;
    this.jiraClient = jiraClient;
    this.jiraConfigMapper = jiraConfigMapper;
    this.jqlBuilder = jqlBuilder;
    this.filterMergeHelper = filterMergeHelper;
  }

  @Override
  public IssueSearchPageDto searchInPod(String podId, IssueSearchFilters filters, int startAt, int maxResults) {
    PodDocument pod = teamService.getActivePodDocument(podId);
    JiraFieldConfig fieldConfig = jiraConfigMapper.toJiraFieldConfig(pod.getJiraConfig());
    List<String> projectKeys = resolveProjectKeys(List.of(pod));
    IssueSearchFilters normalized = filterMergeHelper.normalize(filters);

    return executeSearch(projectKeys, normalized, fieldConfig, startAt, maxResults);
  }

  @Override
  public IssueSearchPageDto searchInRelease(
      String podId, String releaseId, IssueSearchFilters filters, int startAt, int maxResults) {
    PodDocument pod = teamService.getActivePodDocument(podId);
    var release = releaseService.getActiveReleaseDocument(podId, releaseId);

    FilterMergeHelper.MergeResult mergeResult = filterMergeHelper.mergeWithRelease(release, filters);
    if (mergeResult.isNoMatch()) {
      return IssueSearchPageDto.empty(startAt, maxResults);
    }

    JiraFieldConfig fieldConfig = jiraConfigMapper.toJiraFieldConfig(pod.getJiraConfig());
    List<String> projectKeys = resolveProjectKeys(List.of(pod));

    return executeSearch(projectKeys, mergeResult.filters(), fieldConfig, startAt, maxResults);
  }

  private IssueSearchPageDto executeSearch(
      List<String> projectKeys,
      IssueSearchFilters filters,
      JiraFieldConfig fieldConfig,
      int startAt,
      int maxResults) {
    if (projectKeys.isEmpty()) {
      throw new ApiException(
          "POD_JIRA_NOT_CONFIGURED",
          "No Jira project keys configured for search",
          HttpStatus.BAD_REQUEST);
    }

    Optional<String> jql = jqlBuilder.build(projectKeys, filters, fieldConfig);
    if (jql.isEmpty()) {
      return IssueSearchPageDto.empty(startAt, maxResults);
    }

    IssueSearchPage page = jiraClient.searchIssues(jql.get(), fieldConfig, startAt, maxResults);
    return toPageDto(page);
  }

  private List<String> resolveProjectKeys(List<PodDocument> pods) {
    Set<String> projectKeys = new LinkedHashSet<>();
    for (PodDocument pod : pods) {
      if (pod.getJiraConfig() != null && pod.getJiraConfig().getProjectKeys() != null) {
        projectKeys.addAll(pod.getJiraConfig().getProjectKeys());
      }
    }
    return new ArrayList<>(projectKeys);
  }

  private IssueSearchPageDto toPageDto(IssueSearchPage page) {
    return new IssueSearchPageDto(
        page.issues(),
        page.startAt(),
        page.maxResults(),
        page.total(),
        page.last());
  }
}
