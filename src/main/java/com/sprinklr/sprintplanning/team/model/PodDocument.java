package com.sprinklr.sprintplanning.team.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "pods")
@CompoundIndex(name = "team_code_idx", def = "{'teamId': 1, 'code': 1}", unique = true)
public class PodDocument {

  @Id
  private String id;

  @Indexed
  private String teamId;

  private String code;

  private String name;

  private boolean active = true;

  private PodJiraConfig jiraConfig = new PodJiraConfig();

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

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  public PodJiraConfig getJiraConfig() {
    return jiraConfig;
  }

  public void setJiraConfig(PodJiraConfig jiraConfig) {
    this.jiraConfig = jiraConfig;
  }
}
