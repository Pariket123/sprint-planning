package com.sprinklr.sprintplanning.client.jira;

import com.sprinklr.sprintplanning.common.model.BacklogPage;
import com.sprinklr.sprintplanning.common.model.IssueView;
import com.sprinklr.sprintplanning.common.model.JiraFieldConfig;
import com.sprinklr.sprintplanning.common.model.SprintView;

import java.util.List;

public interface JiraClient {

  List<SprintView> getBoardSprints(Long boardId, String state);

  SprintView getSprint(Long sprintId);

  List<IssueView> getSprintIssues(Long sprintId, JiraFieldConfig fieldConfig);

  List<IssueView> getBacklogIssues(Long boardId, JiraFieldConfig fieldConfig);

  BacklogPage getBacklogIssues(Long boardId, JiraFieldConfig fieldConfig, int startAt, int maxResults);

  void moveIssuesToSprint(List<String> issueKeys, Long sprintId);

  void moveIssuesToBacklog(List<String> issueKeys);
}
