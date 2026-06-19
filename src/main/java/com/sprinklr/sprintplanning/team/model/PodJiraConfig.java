package com.sprinklr.sprintplanning.team.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PodJiraConfig {

  private Long boardId;
  private List<String> projectKeys = new ArrayList<>();
  private FieldMappings fieldMappings = new FieldMappings();
  private IssueTypeMappings issueTypeMappings = new IssueTypeMappings();

  public Long getBoardId() {
    return boardId;
  }

  public void setBoardId(Long boardId) {
    this.boardId = boardId;
  }

  public List<String> getProjectKeys() {
    return projectKeys;
  }

  public void setProjectKeys(List<String> projectKeys) {
    this.projectKeys = projectKeys;
  }

  public FieldMappings getFieldMappings() {
    return fieldMappings;
  }

  public void setFieldMappings(FieldMappings fieldMappings) {
    this.fieldMappings = fieldMappings;
  }

  public IssueTypeMappings getIssueTypeMappings() {
    return issueTypeMappings;
  }

  public void setIssueTypeMappings(IssueTypeMappings issueTypeMappings) {
    this.issueTypeMappings = issueTypeMappings;
  }

  public static class FieldMappings {

    private String storyPoints;
    private String domain;
    private Map<String, String> domainValues = new HashMap<>();

    public String getStoryPoints() {
      return storyPoints;
    }

    public void setStoryPoints(String storyPoints) {
      this.storyPoints = storyPoints;
    }

    public String getDomain() {
      return domain;
    }

    public void setDomain(String domain) {
      this.domain = domain;
    }

    public Map<String, String> getDomainValues() {
      return domainValues;
    }

    public void setDomainValues(Map<String, String> domainValues) {
      this.domainValues = domainValues;
    }
  }

  public static class IssueTypeMappings {

    private List<String> bugs = new ArrayList<>();
    private List<String> features = new ArrayList<>();

    public List<String> getBugs() {
      return bugs;
    }

    public void setBugs(List<String> bugs) {
      this.bugs = bugs;
    }

    public List<String> getFeatures() {
      return features;
    }

    public void setFeatures(List<String> features) {
      this.features = features;
    }
  }
}
