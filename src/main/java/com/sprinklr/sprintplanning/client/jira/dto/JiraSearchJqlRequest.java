package com.sprinklr.sprintplanning.client.jira.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class JiraSearchJqlRequest {

  private final String jql;
  private final Integer maxResults;
  private final List<String> fields;
  private final String nextPageToken;

  public JiraSearchJqlRequest(String jql, Integer maxResults, List<String> fields, String nextPageToken) {
    this.jql = jql;
    this.maxResults = maxResults;
    this.fields = fields;
    this.nextPageToken = nextPageToken;
  }

  public String getJql() {
    return jql;
  }

  public Integer getMaxResults() {
    return maxResults;
  }

  public List<String> getFields() {
    return fields;
  }

  public String getNextPageToken() {
    return nextPageToken;
  }
}
