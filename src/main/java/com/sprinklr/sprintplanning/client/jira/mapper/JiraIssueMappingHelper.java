package com.sprinklr.sprintplanning.client.jira.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.sprinklr.sprintplanning.client.jira.dto.JiraIssueDto;
import com.sprinklr.sprintplanning.common.enums.Domain;
import com.sprinklr.sprintplanning.common.enums.StatusCategory;
import com.sprinklr.sprintplanning.common.model.DomainAllocation;
import com.sprinklr.sprintplanning.common.model.JiraFieldConfig;
import org.mapstruct.Context;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class JiraIssueMappingHelper {

  private final JiraFieldReader fieldReader;
  private final DomainMappingService domainMappingService;
  private final SprintFieldParser sprintFieldParser;

  public JiraIssueMappingHelper(
      JiraFieldReader fieldReader,
      DomainMappingService domainMappingService,
      SprintFieldParser sprintFieldParser) {
    this.fieldReader = fieldReader;
    this.domainMappingService = domainMappingService;
    this.sprintFieldParser = sprintFieldParser;
  }

  @Named("resolveSummary")
  public String resolveSummary(JiraIssueDto issue) {
    JsonNode fields = issue.getFields();
    if (fields == null || fields.isMissingNode()) {
      return null;
    }
    return fieldReader.textOrNull(fields.path("summary"));
  }

  @Named("resolveIssueType")
  public String resolveIssueType(JiraIssueDto issue) {
    JsonNode fields = issue.getFields();
    if (fields == null) {
      return null;
    }
    return fieldReader.textOrNull(fields.path("issuetype").path("name"));
  }

  @Named("resolveStatus")
  public String resolveStatus(JiraIssueDto issue) {
    JsonNode fields = issue.getFields();
    if (fields == null) {
      return null;
    }
    return fieldReader.textOrNull(fields.path("status").path("name"));
  }

  @Named("resolveStatusCategory")
  public StatusCategory resolveStatusCategory(JiraIssueDto issue) {
    JsonNode fields = issue.getFields();
    if (fields == null) {
      return StatusCategory.UNKNOWN;
    }
    String key = fieldReader.textOrNull(fields.path("status").path("statusCategory").path("key"));
    return StatusCategory.fromJiraKey(key);
  }

  @Named("resolveStoryPoints")
  public Double resolveStoryPoints(JiraIssueDto issue, @Context JiraFieldConfig config) {
    List<DomainAllocation> allocations = resolveDomainAllocations(issue, config);
    if (!allocations.isEmpty()) {
      return allocations.stream().mapToDouble(DomainAllocation::storyPoints).sum();
    }
    return fieldReader.readNumberField(
        fieldReader.fieldsOrNull(issue),
        config != null ? config.storyPointsFieldId() : null);
  }

  @Named("resolveDomainAllocations")
  public List<DomainAllocation> resolveDomainAllocations(JiraIssueDto issue, @Context JiraFieldConfig config) {
    return domainMappingService.resolveDomainAllocations(issue, config);
  }

  @Named("resolveEngineeringAllocations")
  public List<DomainAllocation> resolveEngineeringAllocations(JiraIssueDto issue, @Context JiraFieldConfig config) {
    return domainMappingService.resolveEngineeringAllocations(issue, config);
  }

  @Named("resolveDomain")
  public Domain resolveDomain(JiraIssueDto issue, @Context JiraFieldConfig config) {
    return domainMappingService.resolveDomain(issue, config);
  }

  @Named("resolveDomainLabel")
  public String resolveDomainLabel(JiraIssueDto issue, @Context JiraFieldConfig config) {
    return domainMappingService.resolveDomainLabel(issue, config);
  }

  @Named("resolveAssigneeId")
  public String resolveAssigneeId(JiraIssueDto issue) {
    JsonNode assignee = fieldReader.fieldsOrNull(issue).path("assignee");
    if (assignee.isMissingNode() || assignee.isNull()) {
      return null;
    }
    return fieldReader.textOrNull(assignee.path("accountId"));
  }

  @Named("resolveAssigneeDisplayName")
  public String resolveAssigneeDisplayName(JiraIssueDto issue) {
    JsonNode assignee = fieldReader.fieldsOrNull(issue).path("assignee");
    if (assignee.isMissingNode() || assignee.isNull()) {
      return null;
    }
    return fieldReader.textOrNull(assignee.path("displayName"));
  }

  @Named("resolvePriority")
  public String resolvePriority(JiraIssueDto issue) {
    JsonNode priority = fieldReader.fieldsOrNull(issue).path("priority");
    if (priority.isMissingNode() || priority.isNull()) {
      return null;
    }
    return fieldReader.textOrNull(priority.path("name"));
  }

  @Named("resolveFixVersions")
  public List<String> resolveFixVersions(JiraIssueDto issue, @Context JiraFieldConfig config) {
    List<String> builtInVersions = fieldReader.readBuiltInFixVersions(issue);
    if (!builtInVersions.isEmpty()) {
      return builtInVersions;
    }

    if (config != null && config.fixVersionFieldId() != null && !config.fixVersionFieldId().isBlank()) {
      String customFixVersion = fieldReader.textOrNull(
          fieldReader.fieldsOrNull(issue).path(config.fixVersionFieldId()));
      if (customFixVersion == null || customFixVersion.isBlank()) {
        return List.of();
      }
      return List.of(customFixVersion.trim());
    }

    return List.of();
  }

  @Named("resolveSprintIds")
  public List<Long> resolveSprintIds(JiraIssueDto issue, @Context JiraFieldConfig config) {
    return sprintFieldParser.resolveSprintIds(fieldReader.fieldsOrNull(issue), config);
  }

  @Named("resolveCurrentSprintId")
  public Long resolveCurrentSprintId(JiraIssueDto issue, @Context JiraFieldConfig config) {
    return sprintFieldParser.resolveCurrentSprintId(fieldReader.fieldsOrNull(issue), config);
  }

  @Named("resolveLabels")
  public List<String> resolveLabels(JiraIssueDto issue) {
    JsonNode labels = fieldReader.fieldsOrNull(issue).path("labels");
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
    JsonNode components = fieldReader.fieldsOrNull(issue).path("components");
    if (!components.isArray()) {
      return List.of();
    }
    List<String> result = new ArrayList<>();
    for (JsonNode component : components) {
      String name = fieldReader.textOrNull(component.path("name"));
      if (name != null) {
        result.add(name);
      }
    }
    return result;
  }
}
