package com.sprinklr.sprintplanning.planning.service;

import com.sprinklr.sprintplanning.common.exception.ApiException;
import com.sprinklr.sprintplanning.team.mapper.JiraConfigMapper;
import com.sprinklr.sprintplanning.team.model.PodDocument;
import com.sprinklr.sprintplanning.team.service.TeamService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class PodJiraContextResolver {

  private final TeamService teamService;
  private final JiraConfigMapper jiraConfigMapper;

  public PodJiraContextResolver(TeamService teamService, JiraConfigMapper jiraConfigMapper) {
    this.teamService = teamService;
    this.jiraConfigMapper = jiraConfigMapper;
  }

  public PodJiraContext resolve(String podId) {
    PodDocument pod = teamService.getActivePodDocument(podId);
    return new PodJiraContext(
        requireBoardId(pod),
        jiraConfigMapper.toJiraFieldConfig(pod.getJiraConfig()));
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
