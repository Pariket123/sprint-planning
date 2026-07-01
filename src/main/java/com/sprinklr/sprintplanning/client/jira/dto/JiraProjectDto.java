package com.sprinklr.sprintplanning.client.jira.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class JiraProjectDto {

  private Long id;
  private String key;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }
}
