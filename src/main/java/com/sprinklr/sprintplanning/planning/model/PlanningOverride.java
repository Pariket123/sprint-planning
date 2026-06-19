package com.sprinklr.sprintplanning.planning.model;

public class PlanningOverride {

  private String issueKey;
  private OverrideAction action;
  private String notes;

  public String getIssueKey() {
    return issueKey;
  }

  public void setIssueKey(String issueKey) {
    this.issueKey = issueKey;
  }

  public OverrideAction getAction() {
    return action;
  }

  public void setAction(OverrideAction action) {
    this.action = action;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }
}
