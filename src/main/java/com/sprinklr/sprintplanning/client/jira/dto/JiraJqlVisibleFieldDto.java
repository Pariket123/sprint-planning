package com.sprinklr.sprintplanning.client.jira.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class JiraJqlVisibleFieldDto {

  private String value;
  private String displayName;
  private String cfid;
  private List<String> operators = new ArrayList<>();

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getCfid() {
    return cfid;
  }

  public void setCfid(String cfid) {
    this.cfid = cfid;
  }

  public List<String> getOperators() {
    return operators;
  }

  public void setOperators(List<String> operators) {
    this.operators = operators != null ? operators : new ArrayList<>();
  }
}
