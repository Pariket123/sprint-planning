package com.sprinklr.sprintplanning.client.jira.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.sprinklr.sprintplanning.client.jira.dto.JiraIssueDto;
import com.sprinklr.sprintplanning.common.enums.Domain;
import com.sprinklr.sprintplanning.common.enums.StatusCategory;
import com.sprinklr.sprintplanning.common.model.JiraFieldConfig;
import org.mapstruct.Context;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class JiraIssueMappingHelper {

  @Named("resolveSummary")
  public String resolveSummary(JiraIssueDto issue) {
    JsonNode fields = issue.getFields();
    if (fields == null || fields.isMissingNode()) {
      return null;
    }
    return textOrNull(fields.path("summary"));
  }

  @Named("resolveIssueType")
  public String resolveIssueType(JiraIssueDto issue) {
    JsonNode fields = issue.getFields();
    if (fields == null) {
      return null;
    }
    return textOrNull(fields.path("issuetype").path("name"));
  }

  @Named("resolveStatus")
  public String resolveStatus(JiraIssueDto issue) {
    JsonNode fields = issue.getFields();
    if (fields == null) {
      return null;
    }
    return textOrNull(fields.path("status").path("name"));
  }

  @Named("resolveStatusCategory")
  public StatusCategory resolveStatusCategory(JiraIssueDto issue) {
    JsonNode fields = issue.getFields();
    if (fields == null) {
      return StatusCategory.UNKNOWN;
    }
    String key = textOrNull(fields.path("status").path("statusCategory").path("key"));
    return StatusCategory.fromJiraKey(key);
  }

  @Named("resolveStoryPoints")
  public Double resolveStoryPoints(JiraIssueDto issue, @Context JiraFieldConfig config) {
    if (config == null || config.storyPointsFieldId() == null) {
      return null;
    }
    JsonNode fields = issue.getFields();
    if (fields == null) {
      return null;
    }
    JsonNode value = fields.path(config.storyPointsFieldId());
    if (value.isMissingNode() || value.isNull()) {
      return null;
    }
    if (value.isNumber()) {
      return value.asDouble();
    }
    try {
      return Double.parseDouble(value.asText());
    } catch (NumberFormatException ex) {
      return null;
    }
  }

  @Named("resolveDomain")
  public Domain resolveDomain(JiraIssueDto issue, @Context JiraFieldConfig config) {
    if (config == null || config.domainFieldId() == null) {
      return Domain.UNKNOWN;
    }
    JsonNode fields = issue.getFields();
    if (fields == null) {
      return Domain.UNKNOWN;
    }
    String jiraValue = extractFieldValue(fields.path(config.domainFieldId()));
    if (jiraValue == null) {
      return Domain.UNKNOWN;
    }
    for (Map.Entry<String, String> entry : config.domainValues().entrySet()) {
      if (jiraValue.equalsIgnoreCase(entry.getValue())) {
        try {
          return Domain.valueOf(entry.getKey());
        } catch (IllegalArgumentException ex) {
          return Domain.UNKNOWN;
        }
      }
    }
    return Domain.UNKNOWN;
  }

  private String extractFieldValue(JsonNode node) {
    if (node.isMissingNode() || node.isNull()) {
      return null;
    }
    if (node.isTextual()) {
      return node.asText();
    }
    if (node.isObject()) {
      if (node.has("value")) {
        return textOrNull(node.path("value"));
      }
      if (node.has("name")) {
        return textOrNull(node.path("name"));
      }
    }
    return node.asText(null);
  }

  private String textOrNull(JsonNode node) {
    if (node == null || node.isMissingNode() || node.isNull()) {
      return null;
    }
    return node.asText();
  }
}
