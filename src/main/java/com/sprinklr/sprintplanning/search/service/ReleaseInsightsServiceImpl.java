package com.sprinklr.sprintplanning.search.service;

import com.sprinklr.sprintplanning.analytics.calculator.AnalyticsCalculator;
import com.sprinklr.sprintplanning.analytics.dto.AnalyticsResponse;
import com.sprinklr.sprintplanning.analytics.workflow.DevSubDomainAnalysisProfiles;
import com.sprinklr.sprintplanning.client.jira.JiraClient;
import com.sprinklr.sprintplanning.common.model.IssueView;
import com.sprinklr.sprintplanning.common.model.JiraFieldConfig;
import com.sprinklr.sprintplanning.planning.calculator.PlanningCalculator;
import com.sprinklr.sprintplanning.release.dto.ReleaseCapacitySummaryDto;
import com.sprinklr.sprintplanning.release.model.ReleaseConfigDocument;
import com.sprinklr.sprintplanning.release.service.ReleaseService;
import com.sprinklr.sprintplanning.search.dto.IssueSearchReleaseRequest;
import com.sprinklr.sprintplanning.search.jql.ReleaseJqlResolver;
import com.sprinklr.sprintplanning.team.mapper.JiraConfigMapper;
import com.sprinklr.sprintplanning.team.model.PodDocument;
import com.sprinklr.sprintplanning.team.service.TeamService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ReleaseInsightsServiceImpl implements ReleaseInsightsService {

  private final TeamService teamService;
  private final ReleaseService releaseService;
  private final JiraClient jiraClient;
  private final JiraConfigMapper jiraConfigMapper;
  private final ReleaseJqlResolver releaseJqlResolver;
  private final AnalyticsCalculator analyticsCalculator;
  private final PlanningCalculator planningCalculator;

  public ReleaseInsightsServiceImpl(
      TeamService teamService,
      ReleaseService releaseService,
      JiraClient jiraClient,
      JiraConfigMapper jiraConfigMapper,
      ReleaseJqlResolver releaseJqlResolver,
      AnalyticsCalculator analyticsCalculator,
      PlanningCalculator planningCalculator) {
    this.teamService = teamService;
    this.releaseService = releaseService;
    this.jiraClient = jiraClient;
    this.jiraConfigMapper = jiraConfigMapper;
    this.releaseJqlResolver = releaseJqlResolver;
    this.analyticsCalculator = analyticsCalculator;
    this.planningCalculator = planningCalculator;
  }

  @Override
  public AnalyticsResponse analyzeRelease(
      String podId,
      String releaseId,
      IssueSearchReleaseRequest request) {
    ReleaseSearchContext context = resolveContext(podId, releaseId, request);
    List<IssueView> allIssues = fetchIssues(context.jql(), context.fieldConfig());
    List<IssueView> issues = filterReleaseAnalyticsIssues(allIssues, context.fieldConfig(), request);

    return analyticsCalculator.calculate(
        null,
        context.release().getName(),
        issues,
        allIssues,
        context.fieldConfig());
  }

  @Override
  public ReleaseCapacitySummaryDto calculateReleaseCapacityMetrics(
      String podId,
      String releaseId,
      IssueSearchReleaseRequest request) {
    ReleaseSearchContext context = resolveContext(podId, releaseId, request);
    List<IssueView> issues = fetchIssues(context.jql(), context.fieldConfig());
    ReleaseConfigDocument release = context.release();

    return planningCalculator.calculateReleaseSummary(
        releaseId,
        release.getDurationDays(),
        release.getStartDate(),
        release.getCapacity(),
        release.getLeavePercent() != null ? release.getLeavePercent() : 0.0,
        release.getCapacityAllocation(),
        issues);
  }

  private ReleaseSearchContext resolveContext(
      String podId,
      String releaseId,
      IssueSearchReleaseRequest request) {
    PodDocument pod = teamService.getActivePodDocument(podId);
    ReleaseConfigDocument release = releaseService.getActiveReleaseDocument(podId, releaseId);
    JiraFieldConfig fieldConfig = jiraConfigMapper.toJiraFieldConfig(pod.getJiraConfig());
    Optional<String> jql = releaseJqlResolver.resolve(pod, release, request, fieldConfig);
    return new ReleaseSearchContext(release, fieldConfig, jql);
  }

  private List<IssueView> fetchIssues(Optional<String> jql, JiraFieldConfig fieldConfig) {
    if (jql.isEmpty()) {
      return List.of();
    }
    return jiraClient.searchAllIssues(jql.get(), fieldConfig);
  }

  private List<IssueView> filterReleaseAnalyticsIssues(
      List<IssueView> issues,
      JiraFieldConfig fieldConfig,
      IssueSearchReleaseRequest request) {
    String profileKey = request != null ? request.issueTypeProfile() : null;
    return DevSubDomainAnalysisProfiles.filterIssues(issues, fieldConfig, profileKey);
  }

  private record ReleaseSearchContext(
      ReleaseConfigDocument release,
      JiraFieldConfig fieldConfig,
      Optional<String> jql) {
  }
}
