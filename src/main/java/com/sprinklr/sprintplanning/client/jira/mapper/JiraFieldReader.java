package com.sprinklr.sprintplanning.client.jira.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.sprinklr.sprintplanning.client.jira.dto.JiraIssueDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class JiraFieldReader {

  public JsonNode fieldsOrNull(JiraIssueDto issue) {
    JsonNode fields = issue.getFields();
    return fields != null ? fields : JsonNodeFactory.instance.objectNode();
  }

  public String textOrNull(JsonNode node) {
    if (node == null || node.isMissingNode() || node.isNull()) {
      return null;
    }
    return node.asText();
  }

  public String extractFieldValue(JsonNode node) {
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

  public Double readNumberField(JsonNode fields, String fieldId) {
    if (fieldId == null || fields == null) {
      return null;
    }
    JsonNode value = fields.path(fieldId);
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

  public double numberOrZero(Double value) {
    return value != null ? value : 0.0;
  }

  public Set<String> readMultiCheckboxOptions(JsonNode node) {
    Set<String> options = new HashSet<>();
    if (node.isMissingNode() || node.isNull()) {
      return options;
    }
    if (node.isArray()) {
      for (JsonNode item : node) {
        String value = extractFieldValue(item);
        if (value != null) {
          options.add(value);
        }
      }
      return options;
    }
    if (node.isBoolean()) {
      if (node.asBoolean()) {
        options.add("true");
      }
      return options;
    }
    String value = extractFieldValue(node);
    if (value != null) {
      options.add(value);
    }
    return options;
  }

  public List<String> readBuiltInFixVersions(JiraIssueDto issue) {
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
}
