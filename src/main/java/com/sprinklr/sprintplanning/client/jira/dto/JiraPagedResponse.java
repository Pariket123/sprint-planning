package com.sprinklr.sprintplanning.client.jira.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class JiraPagedResponse<T> {

  private int startAt;
  private int maxResults;
  private int total;
  private boolean isLast;
  private List<T> values = new ArrayList<>();
  private List<T> issues = new ArrayList<>();

  public int getStartAt() {
    return startAt;
  }

  public void setStartAt(int startAt) {
    this.startAt = startAt;
  }

  public int getMaxResults() {
    return maxResults;
  }

  public void setMaxResults(int maxResults) {
    this.maxResults = maxResults;
  }

  public int getTotal() {
    return total;
  }

  public void setTotal(int total) {
    this.total = total;
  }

  public boolean isLast() {
    return isLast;
  }

  public void setLast(boolean last) {
    isLast = last;
  }

  public List<T> getValues() {
    return values;
  }

  public void setValues(List<T> values) {
    this.values = values;
  }

  public List<T> getIssues() {
    return issues;
  }

  public void setIssues(List<T> issues) {
    this.issues = issues;
  }
}
