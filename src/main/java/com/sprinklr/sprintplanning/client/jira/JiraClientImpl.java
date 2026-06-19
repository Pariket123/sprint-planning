package com.sprinklr.sprintplanning.client.jira;

import com.sprinklr.sprintplanning.client.jira.dto.JiraIssueDto;
import com.sprinklr.sprintplanning.client.jira.dto.JiraPagedResponse;
import com.sprinklr.sprintplanning.client.jira.mapper.JiraIssueMapper;
import com.sprinklr.sprintplanning.client.jira.mapper.JiraSprintMapper;
import com.sprinklr.sprintplanning.client.jira.mapper.JiraTicketMapper;
import com.sprinklr.sprintplanning.common.model.BacklogPage;
import com.sprinklr.sprintplanning.common.model.IssueSearchPage;
import com.sprinklr.sprintplanning.common.model.IssueView;
import com.sprinklr.sprintplanning.common.model.JiraFieldConfig;
import com.sprinklr.sprintplanning.common.model.SprintView;
import com.sprinklr.sprintplanning.search.dto.TicketViewDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class JiraClientImpl implements JiraClient {

  private final JiraRestClient jiraRestClient;
  private final JiraSprintMapper jiraSprintMapper;
  private final JiraIssueMapper jiraIssueMapper;
  private final JiraTicketMapper jiraTicketMapper;

  public JiraClientImpl(
      JiraRestClient jiraRestClient,
      JiraSprintMapper jiraSprintMapper,
      JiraIssueMapper jiraIssueMapper,
      JiraTicketMapper jiraTicketMapper) {
    this.jiraRestClient = jiraRestClient;
    this.jiraSprintMapper = jiraSprintMapper;
    this.jiraIssueMapper = jiraIssueMapper;
    this.jiraTicketMapper = jiraTicketMapper;
  }

  @Override
  public List<SprintView> getBoardSprints(Long boardId, String state) {
    return jiraSprintMapper.toSprintViews(jiraRestClient.getBoardSprints(boardId, state));
  }

  @Override
  public SprintView getSprint(Long sprintId) {
    return jiraSprintMapper.toSprintView(jiraRestClient.getSprint(sprintId));
  }

  @Override
  public List<IssueView> getSprintIssues(Long sprintId, JiraFieldConfig fieldConfig) {
    List<JiraIssueDto> issues = jiraRestClient.getSprintIssues(sprintId, resolveExtraFields(fieldConfig));
    return jiraIssueMapper.toIssueViews(issues, fieldConfig);
  }

  @Override
  public List<IssueView> getBacklogIssues(Long boardId, JiraFieldConfig fieldConfig) {
    List<JiraIssueDto> issues = jiraRestClient.getBacklogIssues(boardId, resolveExtraFields(fieldConfig));
    return jiraIssueMapper.toIssueViews(issues, fieldConfig);
  }

  @Override
  public BacklogPage getBacklogIssues(Long boardId, JiraFieldConfig fieldConfig, int startAt, int maxResults) {
    JiraPagedResponse<JiraIssueDto> page = jiraRestClient.getBacklogIssuesPage(
        boardId, resolveExtraFields(fieldConfig), startAt, maxResults);
    List<JiraIssueDto> issues = page != null && page.getIssues() != null ? page.getIssues() : List.of();
    return new BacklogPage(
        jiraIssueMapper.toIssueViews(issues, fieldConfig),
        page != null ? page.getStartAt() : startAt,
        page != null ? page.getMaxResults() : maxResults,
        page != null ? page.getTotal() : 0,
        page == null || page.isLast());
  }

  @Override
  public void moveIssuesToSprint(List<String> issueKeys, Long sprintId) {
    jiraRestClient.moveIssuesToSprint(issueKeys, sprintId);
  }

  @Override
  public void moveIssuesToBacklog(List<String> issueKeys) {
    jiraRestClient.moveIssuesToBacklog(issueKeys);
  }

  @Override
  public IssueSearchPage searchIssues(String jql, JiraFieldConfig fieldConfig, int startAt, int maxResults) {
    JiraPagedResponse<JiraIssueDto> page = jiraRestClient.searchIssues(
        jql, resolveExtraFields(fieldConfig), startAt, maxResults);
    List<JiraIssueDto> issues = page != null && page.getIssues() != null ? page.getIssues() : List.of();
    return new IssueSearchPage(
        jiraTicketMapper.toTicketViews(issues, fieldConfig),
        page != null ? page.getStartAt() : startAt,
        page != null ? page.getMaxResults() : maxResults,
        page != null ? page.getTotal() : 0,
        page == null || page.isLast());
  }

  @Override
  public List<TicketViewDto> getIssuesByKeys(List<String> issueKeys, JiraFieldConfig fieldConfig) {
    List<JiraIssueDto> issues = jiraRestClient.getIssuesByKeys(issueKeys, resolveExtraFields(fieldConfig));
    return jiraTicketMapper.toTicketViews(issues, fieldConfig);
  }

  private List<String> resolveExtraFields(JiraFieldConfig fieldConfig) {
    if (fieldConfig == null) {
      return List.of();
    }
    Set<String> fields = new LinkedHashSet<>();
    if (fieldConfig.storyPointsFieldId() != null) {
      fields.add(fieldConfig.storyPointsFieldId());
    }
    if (fieldConfig.domainFieldId() != null) {
      fields.add(fieldConfig.domainFieldId());
    }
    return new ArrayList<>(fields);
  }
}
