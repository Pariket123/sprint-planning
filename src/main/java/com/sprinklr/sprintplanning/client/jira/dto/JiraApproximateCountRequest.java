package com.sprinklr.sprintplanning.client.jira.dto;

public class JiraApproximateCountRequest {

  private final String jql;

  public JiraApproximateCountRequest(String jql) {
    this.jql = jql;
  }

  public String getJql() {
    return jql;
  }
}
