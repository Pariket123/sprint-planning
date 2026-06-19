package com.sprinklr.sprintplanning.client.jira.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.sprinklr.sprintplanning.client.jira.dto.JiraIssueDto;
import com.sprinklr.sprintplanning.common.enums.Domain;
import com.sprinklr.sprintplanning.common.enums.StatusCategory;
import com.sprinklr.sprintplanning.common.model.JiraFieldConfig;
import org.mapstruct.Context;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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

  @Named("resolveAssigneeId")
  public String resolveAssigneeId(JiraIssueDto issue) {
    JsonNode assignee = fieldsOrNull(issue).path("assignee");
    if (assignee.isMissingNode() || assignee.isNull()) {
      return null;
    }
    return textOrNull(assignee.path("accountId"));
  }

  @Named("resolveAssigneeDisplayName")
  public String resolveAssigneeDisplayName(JiraIssueDto issue) {
    JsonNode assignee = fieldsOrNull(issue).path("assignee");
    if (assignee.isMissingNode() || assignee.isNull()) {
      return null;
    }
    return textOrNull(assignee.path("displayName"));
  }

  @Named("resolvePriority")
  public String resolvePriority(JiraIssueDto issue) {
    JsonNode priority = fieldsOrNull(issue).path("priority");
    if (priority.isMissingNode() || priority.isNull()) {
      return null;
    }
    return textOrNull(priority.path("name"));
  }

  @Named("resolveFixVersions")
  public List<String> resolveFixVersions(JiraIssueDto issue) {
    JsonNode fixVersions = fieldsOrNull(issue).path("fixVersions");
    if (!fixVersions.isArray()) {
      return List.of();
    }
    List<String> versions = new ArrayList<>();
    for (JsonNode version : fixVersions) {
      String name = textOrNull(version.path("name"));
      if (name != null) {
        versions.add(name);
      }
    }
    return versions;
  }

  @Named("resolveSprintIds")
  public List<Long> resolveSprintIds(JiraIssueDto issue) {
    JsonNode fields = fieldsOrNull(issue);
    if (fields.isMissingNode()) {
      return List.of();
    }
    List<Long> sprintIds = new ArrayList<>();
    Iterator<Map.Entry<String, JsonNode>> fieldIterator = fields.fields();
    while (fieldIterator.hasNext()) {
      Map.Entry<String, JsonNode> entry = fieldIterator.next();
      JsonNode node = entry.getValue();
      if (!node.isArray()) {
        continue;
      }
      for (JsonNode item : node) {
        if (item.has("id") && item.has("state") && item.get("id").isNumber()) {
          sprintIds.add(item.get("id").asLong());
        }
      }
    }
    return sprintIds.stream().distinct().toList();
  }

  @Named("resolveLabels")
  public List<String> resolveLabels(JiraIssueDto issue) {
    JsonNode labels = fieldsOrNull(issue).path("labels");
    if (!labels.isArray()) {
      return List.of();
    }
    List<String> result = new ArrayList<>();
    for (JsonNode label : labels) {
      if (label.isTextual()) {
        result.add(label.asText());
      }
    }
    return result;
  }

  @Named("resolveComponents")
  public List<String> resolveComponents(JiraIssueDto issue) {
    JsonNode components = fieldsOrNull(issue).path("components");
    if (!components.isArray()) {
      return List.of();
    }
    List<String> result = new ArrayList<>();
    for (JsonNode component : components) {
      String name = textOrNull(component.path("name"));
      if (name != null) {
        result.add(name);
      }
    }
    return result;
  }

  private JsonNode fieldsOrNull(JiraIssueDto issue) {
    JsonNode fields = issue.getFields();
    return fields != null ? fields : JsonNodeFactory.instance.objectNode();
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
