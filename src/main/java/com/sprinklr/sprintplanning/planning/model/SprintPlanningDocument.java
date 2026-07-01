package com.sprinklr.sprintplanning.planning.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Document(collection = "sprint_planning")
@CompoundIndex(name = "pod_sprint_idx", def = "{'podId': 1, 'jiraSprintId': 1}", unique = true)
public class SprintPlanningDocument {

  @Id
  private String id;

  private String podId;

  private Long jiraSprintId;

  private List<PersonCapacity> capacity = new ArrayList<>();

  private List<LeaveEntry> leaves = new ArrayList<>();

  private List<PlanningOverride> overrides = new ArrayList<>();

  private Map<String, Double> rolloverStoryPoints = new HashMap<>();

  private List<String> plannedIssueKeys = new ArrayList<>();

  private Instant plannedScopeCapturedAt;

  private List<String> committedIssueKeys = new ArrayList<>();

  private Instant committedAt;

  private List<RolloverIssue> rolloverIssues = new ArrayList<>();

  private List<CapacityAllocationPercents> capacityAllocation = new ArrayList<>();

  private Instant createdAt;

  private Instant updatedAt;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getPodId() {
    return podId;
  }

  public void setPodId(String podId) {
    this.podId = podId;
  }

  public Long getJiraSprintId() {
    return jiraSprintId;
  }

  public void setJiraSprintId(Long jiraSprintId) {
    this.jiraSprintId = jiraSprintId;
  }

  public List<PersonCapacity> getCapacity() {
    return capacity;
  }

  public void setCapacity(List<PersonCapacity> capacity) {
    this.capacity = capacity;
  }

  public List<LeaveEntry> getLeaves() {
    return leaves;
  }

  public void setLeaves(List<LeaveEntry> leaves) {
    this.leaves = leaves;
  }

  public List<PlanningOverride> getOverrides() {
    return overrides;
  }

  public void setOverrides(List<PlanningOverride> overrides) {
    this.overrides = overrides;
  }

  public Map<String, Double> getRolloverStoryPoints() {
    return rolloverStoryPoints;
  }

  public void setRolloverStoryPoints(Map<String, Double> rolloverStoryPoints) {
    this.rolloverStoryPoints = rolloverStoryPoints;
  }

  public List<String> getPlannedIssueKeys() {
    return plannedIssueKeys;
  }

  public void setPlannedIssueKeys(List<String> plannedIssueKeys) {
    this.plannedIssueKeys = plannedIssueKeys;
  }

  public Instant getPlannedScopeCapturedAt() {
    return plannedScopeCapturedAt;
  }

  public void setPlannedScopeCapturedAt(Instant plannedScopeCapturedAt) {
    this.plannedScopeCapturedAt = plannedScopeCapturedAt;
  }

  public List<String> getCommittedIssueKeys() {
    return committedIssueKeys;
  }

  public void setCommittedIssueKeys(List<String> committedIssueKeys) {
    this.committedIssueKeys = committedIssueKeys;
  }

  public Instant getCommittedAt() {
    return committedAt;
  }

  public void setCommittedAt(Instant committedAt) {
    this.committedAt = committedAt;
  }

  public List<RolloverIssue> getRolloverIssues() {
    return rolloverIssues;
  }

  public void setRolloverIssues(List<RolloverIssue> rolloverIssues) {
    this.rolloverIssues = rolloverIssues;
  }

  public List<CapacityAllocationPercents> getCapacityAllocation() {
    return capacityAllocation;
  }

  public void setCapacityAllocation(List<CapacityAllocationPercents> capacityAllocation) {
    this.capacityAllocation = capacityAllocation;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }
}
