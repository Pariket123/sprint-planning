package com.sprinklr.sprintplanning.analytics.service;

import com.sprinklr.sprintplanning.analytics.calculator.AnalyticsCalculator;
import com.sprinklr.sprintplanning.analytics.dto.AnalyticsResponse;
import com.sprinklr.sprintplanning.client.jira.JiraClient;
import com.sprinklr.sprintplanning.common.exception.ApiException;
import com.sprinklr.sprintplanning.analytics.workflow.DevSubDomainAnalysisProfiles;
import com.sprinklr.sprintplanning.common.model.IssueView;
import com.sprinklr.sprintplanning.common.model.JiraFieldConfig;
import com.sprinklr.sprintplanning.common.model.SprintView;
import com.sprinklr.sprintplanning.team.mapper.JiraConfigMapper;
import com.sprinklr.sprintplanning.team.model.PodDocument;
import com.sprinklr.sprintplanning.team.service.TeamService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

@Service
public class AnalyticsServiceImpl implements AnalyticsService {

  private final TeamService teamService;
  private final JiraClient jiraClient;
  private final JiraConfigMapper jiraConfigMapper;
  private final AnalyticsCalculator analyticsCalculator;
  private final Executor jiraFetchExecutor;

  public AnalyticsServiceImpl(
      TeamService teamService,
      JiraClient jiraClient,
      JiraConfigMapper jiraConfigMapper,
      AnalyticsCalculator analyticsCalculator,
      Executor jiraFetchExecutor) {
    this.teamService = teamService;
    this.jiraClient = jiraClient;
    this.jiraConfigMapper = jiraConfigMapper;
    this.analyticsCalculator = analyticsCalculator;
    this.jiraFetchExecutor = jiraFetchExecutor;
  }

  @Override
  public List<SprintView> getSprints(String podId, String state) {
    PodDocument pod = teamService.getActivePodDocument(podId);
    Long boardId = requireBoardId(pod);
    return jiraClient.getBoardSprints(boardId, state);
  }

  @Override
  public AnalyticsResponse getSprintAnalytics(
      String podId,
      Long jiraSprintId,
      String issueTypeProfile) {
    PodDocument pod = teamService.getActivePodDocument(podId);
    JiraFieldConfig fieldConfig = jiraConfigMapper.toJiraFieldConfig(pod.getJiraConfig());

    CompletableFuture<SprintView> sprintFuture = CompletableFuture.supplyAsync(
        () -> jiraClient.getSprint(jiraSprintId), jiraFetchExecutor);
    CompletableFuture<List<IssueView>> issuesFuture = CompletableFuture.supplyAsync(
        () -> jiraClient.searchAllIssues("sprint = " + jiraSprintId, fieldConfig), jiraFetchExecutor);

    SprintView sprint = joinFuture(sprintFuture);
    List<IssueView> allIssues = joinFuture(issuesFuture);
    List<IssueView> issues = DevSubDomainAnalysisProfiles.filterIssues(allIssues, fieldConfig, issueTypeProfile);

    return analyticsCalculator.calculate(jiraSprintId, sprint.name(), issues, allIssues, fieldConfig);
  }

  private static <T> T joinFuture(CompletableFuture<T> future) {
    try {
      return future.join();
    } catch (CompletionException ex) {
      Throwable cause = ex.getCause();
      if (cause instanceof RuntimeException runtimeException) {
        throw runtimeException;
      }
      throw ex;
    }
  }

  private Long requireBoardId(PodDocument pod) {
    Long boardId = pod.getJiraConfig() != null ? pod.getJiraConfig().getBoardId() : null;
    if (boardId == null) {
      throw new ApiException(
          "POD_JIRA_NOT_CONFIGURED",
          "Pod does not have a Jira board configured: " + pod.getId(),
          HttpStatus.BAD_REQUEST);
    }
    return boardId;
  }
}
