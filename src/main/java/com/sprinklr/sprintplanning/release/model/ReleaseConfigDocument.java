package com.sprinklr.sprintplanning.release.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import com.sprinklr.sprintplanning.planning.model.PersonCapacity;
import com.sprinklr.sprintplanning.planning.model.CapacityAllocationPercents;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "release_configs")
@CompoundIndex(name = "team_name_idx", def = "{'teamId': 1, 'name': 1}", unique = true)
public class ReleaseConfigDocument {

  @Id
  private String id;

  @Indexed
  private String teamId;

  private String name;

  private String description;

    private String baseJql;

  private Integer durationDays;

  private LocalDate startDate;

  private List<PersonCapacity> capacity = new ArrayList<>();

  private Double leavePercent = 0.0;

  private List<CapacityAllocationPercents> capacityAllocation = new ArrayList<>();

    private List<String> fixVersionIncludes = new ArrayList<>();

  private List<String> fixVersionExcludes = new ArrayList<>();

  private ReleaseBasicFilters basicFilters = new ReleaseBasicFilters();

  private boolean active = true;

  private Instant createdAt;

  private Instant updatedAt;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getTeamId() {
    return teamId;
  }

  public void setTeamId(String teamId) {
    this.teamId = teamId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getBaseJql() {
    return baseJql;
  }

  public void setBaseJql(String baseJql) {
    this.baseJql = baseJql;
  }

  public Integer getDurationDays() {
    return durationDays;
  }

  public void setDurationDays(Integer durationDays) {
    this.durationDays = durationDays;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public void setStartDate(LocalDate startDate) {
    this.startDate = startDate;
  }

  public List<PersonCapacity> getCapacity() {
    return capacity;
  }

  public void setCapacity(List<PersonCapacity> capacity) {
    this.capacity = capacity;
  }

  public Double getLeavePercent() {
    return leavePercent;
  }

  public void setLeavePercent(Double leavePercent) {
    this.leavePercent = leavePercent;
  }

  public List<CapacityAllocationPercents> getCapacityAllocation() {
    return capacityAllocation;
  }

  public void setCapacityAllocation(List<CapacityAllocationPercents> capacityAllocation) {
    this.capacityAllocation = capacityAllocation;
  }

  public List<String> getFixVersionIncludes() {
    return fixVersionIncludes;
  }

  public void setFixVersionIncludes(List<String> fixVersionIncludes) {
    this.fixVersionIncludes = fixVersionIncludes;
  }

  public List<String> getFixVersionExcludes() {
    return fixVersionExcludes;
  }

  public void setFixVersionExcludes(List<String> fixVersionExcludes) {
    this.fixVersionExcludes = fixVersionExcludes;
  }

  public ReleaseBasicFilters getBasicFilters() {
    return basicFilters;
  }

  public void setBasicFilters(ReleaseBasicFilters basicFilters) {
    this.basicFilters = basicFilters;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
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
