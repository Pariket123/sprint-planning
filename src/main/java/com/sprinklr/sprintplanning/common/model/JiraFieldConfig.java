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
    Map<String, String> domainValues,
    List<String> bugIssueTypes,
    List<String> featureIssueTypes
) {
}
