package com.sprinklr.sprintplanning.team.model;

import com.sprinklr.sprintplanning.common.model.WorkflowAnalysisConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PodJiraConfig {

  private Long boardId;
  private List<String> projectKeys = new ArrayList<>();
  private FieldMappings fieldMappings = new FieldMappings();
  private IssueTypeMappings issueTypeMappings = new IssueTypeMappings();
  private WorkflowAnalysisConfig workflowAnalysis;

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

  public WorkflowAnalysisConfig getWorkflowAnalysis() {
    return workflowAnalysis;
  }

  public void setWorkflowAnalysis(WorkflowAnalysisConfig workflowAnalysis) {
    this.workflowAnalysis = workflowAnalysis;
  }

  public static class FieldMappings {

    private String storyPoints;
    private String domain;
    private String sprint;
    private Map<String, String> domainValues = new HashMap<>();
    private Map<String, String> compositeDomainValues = new HashMap<>();
    private Map<String, String> domainStoryPointFields = new HashMap<>();
    private String domainCompletionField;
    private Map<String, String> domainCompletionValues = new HashMap<>();
    private String fixVersion;

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

    public String getSprint() {
      return sprint;
    }

    public void setSprint(String sprint) {
      this.sprint = sprint;
    }

    public Map<String, String> getDomainValues() {
      return domainValues;
    }

    public void setDomainValues(Map<String, String> domainValues) {
      this.domainValues = domainValues;
    }

    public Map<String, String> getCompositeDomainValues() {
      return compositeDomainValues;
    }

    public void setCompositeDomainValues(Map<String, String> compositeDomainValues) {
      this.compositeDomainValues = compositeDomainValues;
    }

    public Map<String, String> getDomainStoryPointFields() {
      return domainStoryPointFields;
    }

    public void setDomainStoryPointFields(Map<String, String> domainStoryPointFields) {
      this.domainStoryPointFields = domainStoryPointFields;
    }

    public String getDomainCompletionField() {
      return domainCompletionField;
    }

    public void setDomainCompletionField(String domainCompletionField) {
      this.domainCompletionField = domainCompletionField;
    }

    public Map<String, String> getDomainCompletionValues() {
      return domainCompletionValues;
    }

    public void setDomainCompletionValues(Map<String, String> domainCompletionValues) {
      this.domainCompletionValues = domainCompletionValues;
    }

    public String getFixVersion() {
      return fixVersion;
    }

    public void setFixVersion(String fixVersion) {
      this.fixVersion = fixVersion;
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
