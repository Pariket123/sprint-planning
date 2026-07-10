package com.sprinklr.sprintplanning.client.jira;

import com.sprinklr.sprintplanning.client.jira.dto.JiraIssueDto;
import com.sprinklr.sprintplanning.client.jira.dto.JiraPagedResponse;
import com.sprinklr.sprintplanning.client.jira.mapper.JiraIssueMapper;
import com.sprinklr.sprintplanning.client.jira.mapper.JiraSprintMapper;
import com.sprinklr.sprintplanning.client.jira.mapper.JiraTicketMapper;
import com.sprinklr.sprintplanning.common.model.IssueSearchPage;
import com.sprinklr.sprintplanning.common.model.JiraFieldConfig;
import com.sprinklr.sprintplanning.search.dto.TicketViewDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JiraClientImplTest {

  @Mock
  private JiraRestClient jiraRestClient;
  @Mock
  private JiraSprintMapper jiraSprintMapper;
  @Mock
  private JiraIssueMapper jiraIssueMapper;
  @Mock
  private JiraTicketMapper jiraTicketMapper;

  private JiraClient jiraClient;
  private JiraFieldConfig fieldConfig;

  @BeforeEach
  void setUp() {
    jiraClient = new JiraClientImpl(
        jiraRestClient, jiraSprintMapper, jiraIssueMapper, jiraTicketMapper);
    fieldConfig = new JiraFieldConfig(
        "customfield_10016",
        "customfield_10109",
        "customfield_10020",
        Map.of("DEV", "Dev"),
        List.of("Bug"),
        List.of("Story"));
  }

  @Test
  void searchIssuesReturnsPaginatedTicketViews() {
    JiraIssueDto issue = new JiraIssueDto();
    issue.setKey("WFM-1");

    JiraPagedResponse<JiraIssueDto> page = new JiraPagedResponse<>();
    page.setStartAt(0);
    page.setMaxResults(50);
    page.setTotal(1);
    page.setLast(true);
    page.setIssues(List.of(issue));

    TicketViewDto ticket = new TicketViewDto(
        "WFM-1", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

    when(jiraRestClient.searchIssues(eq("project = WFM"), anyList(), eq(0), eq(50))).thenReturn(page);
    when(jiraTicketMapper.toTicketViews(List.of(issue), fieldConfig)).thenReturn(List.of(ticket));

    IssueSearchPage result = jiraClient.searchIssues("project = WFM", fieldConfig, 0, 50);

    assertThat(result.issues()).containsExactly(ticket);
    assertThat(result.startAt()).isZero();
    assertThat(result.maxResults()).isEqualTo(50);
    assertThat(result.total()).isEqualTo(1);
    assertThat(result.last()).isTrue();
  }

  @Test
  void getIssuesByKeysDelegatesToRestClientWithIssueKeys() {
    JiraIssueDto issue = new JiraIssueDto();
    issue.setKey("CARE-105613");
    TicketViewDto ticket = new TicketViewDto(
        "CARE-105613", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

    when(jiraRestClient.getIssuesByKeys(eq(List.of("CARE-105613", "CARE-105614")), anyList()))
        .thenReturn(List.of(issue));
    when(jiraTicketMapper.toTicketViews(List.of(issue), fieldConfig)).thenReturn(List.of(ticket));

    List<TicketViewDto> result = jiraClient.getIssuesByKeys(
        List.of("CARE-105613", "CARE-105614"), fieldConfig);

    assertThat(result).containsExactly(ticket);

    ArgumentCaptor<List<String>> keysCaptor = ArgumentCaptor.forClass(List.class);
    verify(jiraRestClient).getIssuesByKeys(keysCaptor.capture(), anyList());
    assertThat(keysCaptor.getValue()).containsExactly("CARE-105613", "CARE-105614");
  }

  @Test
  void getIssuesByKeysRequestsSprintFieldAlongWithConfiguredCustomFields() {
    JiraIssueDto issue = new JiraIssueDto();
    issue.setKey("CARE-105613");
    TicketViewDto ticket = new TicketViewDto(
        "CARE-105613", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

    when(jiraRestClient.getIssuesByKeys(eq(List.of("CARE-105613")), anyList()))
        .thenReturn(List.of(issue));
    when(jiraTicketMapper.toTicketViews(List.of(issue), fieldConfig)).thenReturn(List.of(ticket));

    jiraClient.getIssuesByKeys(List.of("CARE-105613"), fieldConfig);

    ArgumentCaptor<List<String>> fieldsCaptor = ArgumentCaptor.forClass(List.class);
    verify(jiraRestClient).getIssuesByKeys(eq(List.of("CARE-105613")), fieldsCaptor.capture());
    assertThat(fieldsCaptor.getValue())
        .contains("customfield_10016", "customfield_10109", "customfield_10020");
  }

  @Test
  void getIssuesByKeysUsesDefaultSprintFieldWhenConfigOmitsIt() {
    JiraFieldConfig configWithoutSprint = new JiraFieldConfig(
        "customfield_10016",
        "customfield_10109",
        null,
        Map.of("DEV", "Dev"),
        List.of("Bug"),
        List.of("Story"));

    when(jiraRestClient.getIssuesByKeys(eq(List.of("WFM-1")), anyList())).thenReturn(List.of());
    when(jiraTicketMapper.toTicketViews(eq(List.of()), eq(configWithoutSprint))).thenReturn(List.of());

    jiraClient.getIssuesByKeys(List.of("WFM-1"), configWithoutSprint);

    ArgumentCaptor<List<String>> fieldsCaptor = ArgumentCaptor.forClass(List.class);
    verify(jiraRestClient).getIssuesByKeys(eq(List.of("WFM-1")), fieldsCaptor.capture());
    assertThat(fieldsCaptor.getValue()).contains(JiraClientImpl.DEFAULT_SPRINT_FIELD_ID);
  }

  @Test
  void getIssuesByKeysReturnsEmptyListForEmptyInput() {
    when(jiraRestClient.getIssuesByKeys(eq(List.of()), anyList())).thenReturn(List.of());
    when(jiraTicketMapper.toTicketViews(eq(List.of()), eq(fieldConfig))).thenReturn(List.of());

    List<TicketViewDto> result = jiraClient.getIssuesByKeys(List.of(), fieldConfig);

    assertThat(result).isEmpty();
  }
}
