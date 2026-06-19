package com.sprinklr.sprintplanning.client.jira.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

@JsonIgnoreProperties(ignoreUnknown = true)
public class JiraIssueDto {

  private String key;
  private JsonNode fields;

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public JsonNode getFields() {
    return fields;
  }

  public void setFields(JsonNode fields) {
    this.fields = fields;
  }
}
