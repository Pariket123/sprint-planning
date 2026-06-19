package com.sprinklr.sprintplanning.client.jira.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class JiraSearchRequest {

  private final String jql;
  private final int startAt;
  private final int maxResults;
  private final List<String> fields;

  public JiraSearchRequest(String jql, int startAt, int maxResults, List<String> fields) {
    this.jql = jql;
    this.startAt = startAt;
    this.maxResults = maxResults;
    this.fields = fields;
  }

  public String getJql() {
    return jql;
  }

  public int getStartAt() {
    return startAt;
  }

  public int getMaxResults() {
    return maxResults;
  }

  public List<String> getFields() {
    return fields;
  }
}
