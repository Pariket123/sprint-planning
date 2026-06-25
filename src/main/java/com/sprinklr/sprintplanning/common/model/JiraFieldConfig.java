package com.sprinklr.sprintplanning.common.model;

import java.util.List;
import java.util.Map;

/**
 * Neutral Jira field configuration used by the client layer for issue mapping.
 * Populated from per-pod MongoDB config.
 */
public record JiraFieldConfig(
    String storyPointsFieldId,
    String domainFieldId,
    String sprintFieldId,
    Map<String, String> domainValues,
    List<String> bugIssueTypes,
    List<String> featureIssueTypes,
    Map<String, String> compositeDomainValues,
    Map<String, String> domainStoryPointFields,
    String domainCompletionFieldId,
    Map<String, String> domainCompletionValues
) {
  public JiraFieldConfig(
      String storyPointsFieldId,
      String domainFieldId,
      String sprintFieldId,
      Map<String, String> domainValues,
      List<String> bugIssueTypes,
      List<String> featureIssueTypes) {
    this(
        storyPointsFieldId,
        domainFieldId,
        sprintFieldId,
        domainValues,
        bugIssueTypes,
        featureIssueTypes,
        Map.of(),
        Map.of(),
        null,
        Map.of());
  }

  public boolean hasMultiDomainSupport() {
    return domainStoryPointFields != null && !domainStoryPointFields.isEmpty();
  }
}
