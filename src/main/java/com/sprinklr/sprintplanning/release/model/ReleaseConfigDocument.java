package com.sprinklr.sprintplanning.release.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "release_configs")
@CompoundIndex(name = "pod_name_idx", def = "{'podId': 1, 'name': 1}", unique = true)
public class ReleaseConfigDocument {

  @Id
  private String id;

  @Indexed
  private String teamId;

  @Indexed
  private String podId;

  private String name;

  private String description;

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

  public String getPodId() {
    return podId;
  }

  public void setPodId(String podId) {
    this.podId = podId;
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
