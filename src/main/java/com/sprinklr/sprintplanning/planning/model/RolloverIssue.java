package com.sprinklr.sprintplanning.planning.model;

import com.sprinklr.sprintplanning.common.enums.Domain;

import java.time.Instant;

public class RolloverIssue {

  private String issueKey;

  private Long fromSprintId;

  private Long toSprintId;

  private String statusAtRollover;

  private Double storyPointsAtRollover;

  private Domain domain;

  private String domainLabel;

  private Instant rolledOverAt;

  private String rolledOverBy;

  private String notes;

  public String getIssueKey() {
    return issueKey;
  }

  public void setIssueKey(String issueKey) {
    this.issueKey = issueKey;
  }

  public Long getFromSprintId() {
    return fromSprintId;
  }

  public void setFromSprintId(Long fromSprintId) {
    this.fromSprintId = fromSprintId;
  }

  public Long getToSprintId() {
    return toSprintId;
  }

  public void setToSprintId(Long toSprintId) {
    this.toSprintId = toSprintId;
  }

  public String getStatusAtRollover() {
    return statusAtRollover;
  }

  public void setStatusAtRollover(String statusAtRollover) {
    this.statusAtRollover = statusAtRollover;
  }

  public Double getStoryPointsAtRollover() {
    return storyPointsAtRollover;
  }

  public void setStoryPointsAtRollover(Double storyPointsAtRollover) {
    this.storyPointsAtRollover = storyPointsAtRollover;
  }

  public Domain getDomain() {
    return domain;
  }

  public void setDomain(Domain domain) {
    this.domain = domain;
  }

  public String getDomainLabel() {
    return domainLabel;
  }

  public void setDomainLabel(String domainLabel) {
    this.domainLabel = domainLabel;
  }

  public Instant getRolledOverAt() {
    return rolledOverAt;
  }

  public void setRolledOverAt(Instant rolledOverAt) {
    this.rolledOverAt = rolledOverAt;
  }

  public String getRolledOverBy() {
    return rolledOverBy;
  }

  public void setRolledOverBy(String rolledOverBy) {
    this.rolledOverBy = rolledOverBy;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }
}
