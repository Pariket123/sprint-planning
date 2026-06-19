package com.sprinklr.sprintplanning.release.model;

import java.util.ArrayList;
import java.util.List;

public class ReleaseBasicFilters {

  private List<String> issueTypes = new ArrayList<>();
  private List<String> statuses = new ArrayList<>();
  private List<String> domains = new ArrayList<>();
  private List<String> priorities = new ArrayList<>();
  private List<String> assigneeIds = new ArrayList<>();

  public List<String> getIssueTypes() {
    return issueTypes;
  }

  public void setIssueTypes(List<String> issueTypes) {
    this.issueTypes = issueTypes;
  }

  public List<String> getStatuses() {
    return statuses;
  }

  public void setStatuses(List<String> statuses) {
    this.statuses = statuses;
  }

  public List<String> getDomains() {
    return domains;
  }

  public void setDomains(List<String> domains) {
    this.domains = domains;
  }

  public List<String> getPriorities() {
    return priorities;
  }

  public void setPriorities(List<String> priorities) {
    this.priorities = priorities;
  }

  public List<String> getAssigneeIds() {
    return assigneeIds;
  }

  public void setAssigneeIds(List<String> assigneeIds) {
    this.assigneeIds = assigneeIds;
  }
}
