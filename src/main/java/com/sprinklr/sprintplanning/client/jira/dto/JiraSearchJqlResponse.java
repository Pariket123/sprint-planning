package com.sprinklr.sprintplanning.client.jira.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class JiraSearchJqlResponse {

  private List<JiraIssueDto> issues = new ArrayList<>();
  private boolean isLast;
  private String nextPageToken;

  public List<JiraIssueDto> getIssues() {
    return issues;
  }

  public void setIssues(List<JiraIssueDto> issues) {
    this.issues = issues;
  }

  public boolean isLast() {
    return isLast;
  }

  public void setLast(boolean last) {
    isLast = last;
  }

  public String getNextPageToken() {
    return nextPageToken;
  }

  public void setNextPageToken(String nextPageToken) {
    this.nextPageToken = nextPageToken;
  }
}
