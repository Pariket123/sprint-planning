package com.sprinklr.sprintplanning.search.service;

import com.sprinklr.sprintplanning.analytics.dto.AnalyticsResponse;
import com.sprinklr.sprintplanning.client.jira.JiraClient;
import com.sprinklr.sprintplanning.common.exception.ApiException;
import com.sprinklr.sprintplanning.common.model.IssueSearchPage;
import com.sprinklr.sprintplanning.common.model.JiraFieldConfig;
import com.sprinklr.sprintplanning.release.dto.ReleaseCapacitySummaryDto;
import com.sprinklr.sprintplanning.release.model.ReleaseConfigDocument;
import com.sprinklr.sprintplanning.release.service.ReleaseService;
import com.sprinklr.sprintplanning.search.FilterMergeHelper;
import com.sprinklr.sprintplanning.search.dto.IssueSearchFilters;
import com.sprinklr.sprintplanning.search.dto.IssueSearchPageDto;
import com.sprinklr.sprintplanning.search.dto.IssueSearchReleaseRequest;
import com.sprinklr.sprintplanning.search.jql.JqlBuilder;
import com.sprinklr.sprintplanning.search.jql.ReleaseJqlResolver;
import com.sprinklr.sprintplanning.search.support.PodProjectKeyResolver;
import com.sprinklr.sprintplanning.team.mapper.JiraConfigMapper;
import com.sprinklr.sprintplanning.team.model.PodDocument;
import com.sprinklr.sprintplanning.team.service.TeamService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class IssueSearchServiceImpl implements IssueSearchService {

  private final TeamService teamService;
  private final ReleaseService releaseService;
  private final JiraClient jiraClient;
  private final JiraConfigMapper jiraConfigMapper;
  private final JqlBuilder jqlBuilder;
  private final FilterMergeHelper filterMergeHelper;
  private final ReleaseJqlResolver releaseJqlResolver;
  private final PodProjectKeyResolver podProjectKeyResolver;
  private final ReleaseInsightsService releaseInsightsService;

  public IssueSearchServiceImpl(
      TeamService teamService,
      ReleaseService releaseService,
      JiraClient jiraClient,
      JiraConfigMapper jiraConfigMapper,
      JqlBuilder jqlBuilder,
      FilterMergeHelper filterMergeHelper,
      ReleaseJqlResolver releaseJqlResolver,
      PodProjectKeyResolver podProjectKeyResolver,
      ReleaseInsightsService releaseInsightsService) {
    this.teamService = teamService;
    this.releaseService = releaseService;
    this.jiraClient = jiraClient;
    this.jiraConfigMapper = jiraConfigMapper;
    this.jqlBuilder = jqlBuilder;
    this.filterMergeHelper = filterMergeHelper;
    this.releaseJqlResolver = releaseJqlResolver;
    this.podProjectKeyResolver = podProjectKeyResolver;
    this.releaseInsightsService = releaseInsightsService;
  }

  @Override
  public IssueSearchPageDto searchInPod(String podId, IssueSearchFilters filters, int startAt, int maxResults) {
    PodDocument pod = teamService.getActivePodDocument(podId);
    JiraFieldConfig fieldConfig = jiraConfigMapper.toJiraFieldConfig(pod.getJiraConfig());
    List<String> projectKeys = podProjectKeyResolver.resolveProjectKeys(List.of(pod));
    IssueSearchFilters normalized = filterMergeHelper.normalize(filters);

    return executeFilterSearch(projectKeys, normalized, fieldConfig, startAt, maxResults);
  }

  @Override
  public IssueSearchPageDto searchInRelease(
      String podId,
      String releaseId,
      IssueSearchReleaseRequest request,
      int startAt,
      int maxResults) {
    PodDocument pod = teamService.getActivePodDocument(podId);
    ReleaseConfigDocument release = releaseService.getActiveReleaseDocument(podId, releaseId);
    JiraFieldConfig fieldConfig = jiraConfigMapper.toJiraFieldConfig(pod.getJiraConfig());

    Optional<String> jql = releaseJqlResolver.resolve(pod, release, request, fieldConfig);
    if (jql.isEmpty()) {
      return IssueSearchPageDto.empty(startAt, maxResults);
    }

    return executeJqlSearch(jql.get(), fieldConfig, startAt, maxResults);
  }

  @Override
  public AnalyticsResponse analyzeRelease(
      String podId,
      String releaseId,
      IssueSearchReleaseRequest request) {
    return releaseInsightsService.analyzeRelease(podId, releaseId, request);
  }

  @Override
  public ReleaseCapacitySummaryDto calculateReleaseCapacityMetrics(
      String podId,
      String releaseId,
      IssueSearchReleaseRequest request) {
    return releaseInsightsService.calculateReleaseCapacityMetrics(podId, releaseId, request);
  }

  private IssueSearchPageDto executeFilterSearch(
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

    return executeJqlSearch(jql.get(), fieldConfig, startAt, maxResults);
  }

  private IssueSearchPageDto executeJqlSearch(
      String jql,
      JiraFieldConfig fieldConfig,
      int startAt,
      int maxResults) {
    IssueSearchPage page = jiraClient.searchIssues(jql, fieldConfig, startAt, maxResults);
    return new IssueSearchPageDto(
        page.issues(),
        page.startAt(),
        page.maxResults(),
        page.total(),
        page.last());
  }
}
