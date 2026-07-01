package com.sprinklr.sprintplanning.client.jira.dto;

import java.util.ArrayList;
import java.util.List;

public class JiraJqlAutocompleteDataRequest {

  private boolean includeCollapsedFields = true;
  private List<Long> projectIds = new ArrayList<>();

  public JiraJqlAutocompleteDataRequest() {
  }

  public JiraJqlAutocompleteDataRequest(boolean includeCollapsedFields, List<Long> projectIds) {
    this.includeCollapsedFields = includeCollapsedFields;
    this.projectIds = projectIds != null ? projectIds : new ArrayList<>();
  }

  public boolean isIncludeCollapsedFields() {
    return includeCollapsedFields;
  }

  public void setIncludeCollapsedFields(boolean includeCollapsedFields) {
    this.includeCollapsedFields = includeCollapsedFields;
  }

  public List<Long> getProjectIds() {
    return projectIds;
  }

  public void setProjectIds(List<Long> projectIds) {
    this.projectIds = projectIds != null ? projectIds : new ArrayList<>();
  }
}
