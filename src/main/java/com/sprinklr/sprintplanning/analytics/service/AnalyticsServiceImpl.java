package com.sprinklr.sprintplanning.analytics.service;

import com.sprinklr.sprintplanning.analytics.calculator.AnalyticsCalculator;
import com.sprinklr.sprintplanning.analytics.dto.AnalyticsResponse;
import com.sprinklr.sprintplanning.client.jira.JiraClient;
import com.sprinklr.sprintplanning.common.exception.ApiException;
import com.sprinklr.sprintplanning.common.model.JiraFieldConfig;
import com.sprinklr.sprintplanning.common.model.SprintView;
import com.sprinklr.sprintplanning.team.mapper.JiraConfigMapper;
import com.sprinklr.sprintplanning.team.model.PodDocument;
import com.sprinklr.sprintplanning.team.service.TeamService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AnalyticsServiceImpl implements AnalyticsService {

  private final TeamService teamService;
  private final JiraClient jiraClient;
  private final JiraConfigMapper jiraConfigMapper;
  private final AnalyticsCalculator analyticsCalculator;

  public AnalyticsServiceImpl(
      TeamService teamService,
      JiraClient jiraClient,
      JiraConfigMapper jiraConfigMapper,
      AnalyticsCalculator analyticsCalculator) {
    this.teamService = teamService;
    this.jiraClient = jiraClient;
    this.jiraConfigMapper = jiraConfigMapper;
    this.analyticsCalculator = analyticsCalculator;
  }

  @Override
  public List<SprintView> getSprints(String podId, String state) {
    PodDocument pod = teamService.getActivePodDocument(podId);
    Long boardId = requireBoardId(pod);
    return jiraClient.getBoardSprints(boardId, state);
  }

  @Override
  public AnalyticsResponse getSprintAnalytics(String podId, Long jiraSprintId) {
    PodDocument pod = teamService.getActivePodDocument(podId);
    JiraFieldConfig fieldConfig = jiraConfigMapper.toJiraFieldConfig(pod.getJiraConfig());

    SprintView sprint = jiraClient.getSprint(jiraSprintId);
    var issues = jiraClient.getSprintIssues(jiraSprintId, fieldConfig);

    return analyticsCalculator.calculate(jiraSprintId, sprint.name(), issues, fieldConfig);
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
