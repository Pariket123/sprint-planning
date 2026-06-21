package com.sprinklr.sprintplanning.client.jira;

import com.sprinklr.sprintplanning.client.jira.dto.JiraIssueDto;
import com.sprinklr.sprintplanning.client.jira.dto.JiraPagedResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.POST;

class JiraRestClientTest {

  private MockRestServiceServer server;
  private JiraRestClient jiraRestClient;

  @BeforeEach
  void setUp() {
    RestClient.Builder builder = RestClient.builder().baseUrl("https://jira.example.com");
    server = MockRestServiceServer.bindTo(builder).build();
    RestClient restClient = builder.build();
    jiraRestClient = new JiraRestClient(restClient, RetryTemplate.builder().maxAttempts(1).build());
  }

  @Test
  void searchIssuesUsesSearchJqlEndpoint() {
    server.expect(requestTo("https://jira.example.com/rest/api/3/search/jql"))
        .andExpect(method(POST))
        .andExpect(jsonPath("$.jql").value("project IN (\"SCRUM\") ORDER BY updated DESC"))
        .andExpect(jsonPath("$.maxResults").value(50))
        .andRespond(withSuccess("""
            {
              "issues": [
                { "key": "SCRUM-1", "fields": { "summary": "Issue one" } }
              ],
              "isLast": true
            }
            """, MediaType.APPLICATION_JSON));

    server.expect(requestTo("https://jira.example.com/rest/api/3/search/approximate-count"))
        .andExpect(method(POST))
        .andExpect(jsonPath("$.jql").value("project IN (\"SCRUM\") ORDER BY updated DESC"))
        .andRespond(withSuccess("""
            { "count": 1 }
            """, MediaType.APPLICATION_JSON));

    JiraPagedResponse<JiraIssueDto> page = jiraRestClient.searchIssues(
        "project IN (\"SCRUM\") ORDER BY updated DESC", List.of(), 0, 50);

    assertThat(page.getIssues()).hasSize(1);
    assertThat(page.getIssues().get(0).getKey()).isEqualTo("SCRUM-1");
    assertThat(page.getTotal()).isEqualTo(1);
    assertThat(page.isLast()).isTrue();
    server.verify();
  }

  @Test
  void getIssuesByKeysUsesSearchJqlWithKeyClause() {
    server.expect(requestTo("https://jira.example.com/rest/api/3/search/jql"))
        .andExpect(method(POST))
        .andExpect(jsonPath("$.jql").value("key in (\"CARE-1\",\"CARE-2\")"))
        .andRespond(withSuccess("""
            {
              "issues": [
                { "key": "CARE-1", "fields": {} },
                { "key": "CARE-2", "fields": {} }
              ],
              "isLast": true
            }
            """, MediaType.APPLICATION_JSON));

    List<JiraIssueDto> issues = jiraRestClient.getIssuesByKeys(
        List.of("CARE-1", "CARE-2"), List.of("customfield_10016"));

    assertThat(issues).extracting(JiraIssueDto::getKey).containsExactly("CARE-1", "CARE-2");
    server.verify();
  }
}
