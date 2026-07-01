package com.sprinklr.sprintplanning.search.service;

import com.sprinklr.sprintplanning.analytics.calculator.AnalyticsCalculator;
import com.sprinklr.sprintplanning.analytics.workflow.DevSubDomainAnalysisProfiles;
import com.sprinklr.sprintplanning.planning.calculator.PlanningCalculator;
import com.sprinklr.sprintplanning.release.dto.ReleaseCapacitySummaryDto;
import com.sprinklr.sprintplanning.analytics.dto.AnalyticsResponse;
import com.sprinklr.sprintplanning.client.jira.JiraClient;
import com.sprinklr.sprintplanning.common.exception.ApiException;
import com.sprinklr.sprintplanning.common.model.IssueSearchPage;
import com.sprinklr.sprintplanning.common.model.IssueView;
import com.sprinklr.sprintplanning.common.model.JiraFieldConfig;
import com.sprinklr.sprintplanning.release.model.ReleaseConfigDocument;
import com.sprinklr.sprintplanning.release.service.ReleaseService;
import com.sprinklr.sprintplanning.search.FilterMergeHelper;
import com.sprinklr.sprintplanning.search.dto.IssueSearchFilters;
import com.sprinklr.sprintplanning.search.dto.IssueSearchPageDto;
import com.sprinklr.sprintplanning.search.dto.IssueSearchReleaseRequest;
import com.sprinklr.sprintplanning.search.jql.JqlBuilder;
import com.sprinklr.sprintplanning.search.jql.JqlMergeHelper;
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
  private final JqlMergeHelper jqlMergeHelper;
  private final FilterMergeHelper filterMergeHelper;
  private final AnalyticsCalculator analyticsCalculator;
  private final PlanningCalculator planningCalculator;

  public IssueSearchServiceImpl(
      TeamService teamService,
      ReleaseService releaseService,
      JiraClient jiraClient,
      JiraConfigMapper jiraConfigMapper,
      JqlBuilder jqlBuilder,
      JqlMergeHelper jqlMergeHelper,
      FilterMergeHelper filterMergeHelper,
      AnalyticsCalculator analyticsCalculator,
      PlanningCalculator planningCalculator) {
    this.teamService = teamService;
    this.releaseService = releaseService;
    this.jiraClient = jiraClient;
    this.jiraConfigMapper = jiraConfigMapper;
    this.jqlBuilder = jqlBuilder;
    this.jqlMergeHelper = jqlMergeHelper;
    this.filterMergeHelper = filterMergeHelper;
    this.analyticsCalculator = analyticsCalculator;
    this.planningCalculator = planningCalculator;
  }

  @Override
  public IssueSearchPageDto searchInPod(String podId, IssueSearchFilters filters, int startAt, int maxResults) {
    PodDocument pod = teamService.getActivePodDocument(podId);
    JiraFieldConfig fieldConfig = jiraConfigMapper.toJiraFieldConfig(pod.getJiraConfig());
    List<String> projectKeys = resolveProjectKeys(List.of(pod));
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

    Optional<String> jql = resolveReleaseJql(pod, release, request, fieldConfig);
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
    PodDocument pod = teamService.getActivePodDocument(podId);
    ReleaseConfigDocument release = releaseService.getActiveReleaseDocument(podId, releaseId);
    JiraFieldConfig fieldConfig = jiraConfigMapper.toJiraFieldConfig(pod.getJiraConfig());

    Optional<String> jql = resolveReleaseJql(pod, release, request, fieldConfig);
    List<IssueView> allIssues = jql.isEmpty()
        ? List.of()
        : jiraClient.searchAllIssues(jql.get(), fieldConfig);
    List<IssueView> issues = filterReleaseAnalyticsIssues(allIssues, fieldConfig, request);

    return analyticsCalculator.calculate(null, release.getName(), issues, allIssues, fieldConfig);
  }

  @Override
  public ReleaseCapacitySummaryDto calculateReleaseCapacityMetrics(
      String podId,
      String releaseId,
      IssueSearchReleaseRequest request) {
    PodDocument pod = teamService.getActivePodDocument(podId);
    ReleaseConfigDocument release = releaseService.getActiveReleaseDocument(podId, releaseId);
    JiraFieldConfig fieldConfig = jiraConfigMapper.toJiraFieldConfig(pod.getJiraConfig());

    Optional<String> jql = resolveReleaseJql(pod, release, request, fieldConfig);
    List<IssueView> issues = jql.isEmpty()
        ? List.of()
        : jiraClient.searchAllIssues(jql.get(), fieldConfig);

    return planningCalculator.calculateReleaseSummary(
        releaseId,
        release.getDurationDays(),
        release.getStartDate(),
        release.getCapacity(),
        release.getLeavePercent() != null ? release.getLeavePercent() : 0.0,
        release.getCapacityAllocation(),
        issues);
  }

  private List<IssueView> filterReleaseAnalyticsIssues(
      List<IssueView> issues,
      JiraFieldConfig fieldConfig,
      IssueSearchReleaseRequest request) {
    String profileKey = request != null ? request.issueTypeProfile() : null;
    return DevSubDomainAnalysisProfiles.filterIssues(issues, fieldConfig, profileKey);
  }

  private Optional<String> resolveReleaseJql(
      PodDocument pod,
      ReleaseConfigDocument release,
      IssueSearchReleaseRequest request,
      JiraFieldConfig fieldConfig) {
    String baseJql = release.getBaseJql();
    if (baseJql != null && !baseJql.isBlank()) {
      String additionalJql = request != null ? request.additionalJql() : null;
      String mergedJql = jqlMergeHelper.merge(baseJql, additionalJql);
      if (mergedJql == null || mergedJql.isBlank()) {
        return Optional.empty();
      }
      return Optional.of(mergedJql);
    }

    FilterMergeHelper.MergeResult mergeResult =
        filterMergeHelper.mergeWithRelease(release, IssueSearchFilters.empty());
    if (mergeResult.isNoMatch()) {
      return Optional.empty();
    }

    List<String> projectKeys = resolveProjectKeys(List.of(pod));
    return jqlBuilder.build(projectKeys, mergeResult.filters(), fieldConfig);
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
