package com.sprinklr.sprintplanning.client.jira.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class JiraJqlReferenceDataDto {

  private List<String> jqlReservedWords = new ArrayList<>();
  private List<JiraJqlVisibleFieldDto> visibleFieldNames = new ArrayList<>();
  private List<JiraJqlVisibleFunctionDto> visibleFunctionNames = new ArrayList<>();

  public List<String> getJqlReservedWords() {
    return jqlReservedWords;
  }

  public void setJqlReservedWords(List<String> jqlReservedWords) {
    this.jqlReservedWords = jqlReservedWords != null ? jqlReservedWords : new ArrayList<>();
  }

  public List<JiraJqlVisibleFieldDto> getVisibleFieldNames() {
    return visibleFieldNames;
  }

  public void setVisibleFieldNames(List<JiraJqlVisibleFieldDto> visibleFieldNames) {
    this.visibleFieldNames = visibleFieldNames != null ? visibleFieldNames : new ArrayList<>();
  }

  public List<JiraJqlVisibleFunctionDto> getVisibleFunctionNames() {
    return visibleFunctionNames;
  }

  public void setVisibleFunctionNames(List<JiraJqlVisibleFunctionDto> visibleFunctionNames) {
    this.visibleFunctionNames = visibleFunctionNames != null ? visibleFunctionNames : new ArrayList<>();
  }
}
