package com.sprinklr.sprintplanning.search.jql;

import com.sprinklr.sprintplanning.common.model.JiraFieldConfig;
import com.sprinklr.sprintplanning.search.dto.IssueSearchFilters;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class JqlBuilder {

  public Optional<String> build(List<String> projectKeys, IssueSearchFilters filters, JiraFieldConfig fieldConfig) {
    if (filters == null) {
      filters = IssueSearchFilters.empty();
    }

    List<String> clauses = new ArrayList<>();

    if (projectKeys != null && !projectKeys.isEmpty()) {
      clauses.add(inClause("project", quoteAll(projectKeys)));
    }

    addInClause(clauses, "issuetype", filters.issueTypes());
    addInClause(clauses, "status", filters.statuses());
    addInClause(clauses, "priority", filters.priorities());
    addInClause(clauses, "labels", filters.labels());
    addInClause(clauses, "component", filters.components());
    addInClause(clauses, "key", filters.issueKeys());
    addInClause(clauses, "assignee", filters.assigneeIds());
    addInClause(clauses, resolveFixVersionField(fieldConfig), filters.fixVersions());
    addNotInClause(clauses, resolveFixVersionField(fieldConfig), filters.fixVersionExcludes());

    if (filters.sprintIds() != null && !filters.sprintIds().isEmpty()) {
      List<String> sprintIds = filters.sprintIds().stream()
          .map(String::valueOf)
          .toList();
      clauses.add(inClause("sprint", sprintIds));
    }

    Optional<String> domainClause = buildDomainClause(filters.domains(), fieldConfig);
    if (domainClause.isEmpty() && filters.domains() != null && !filters.domains().isEmpty()) {
      return Optional.empty();
    }
    domainClause.ifPresent(clauses::add);

    if (clauses.isEmpty()) {
      return Optional.of("order by updated DESC");
    }

    return Optional.of(String.join(" AND ", clauses) + " ORDER BY updated DESC");
  }

  private Optional<String> buildDomainClause(List<String> domains, JiraFieldConfig fieldConfig) {
    if (domains == null || domains.isEmpty()) {
      return Optional.empty();
    }
    if (fieldConfig == null || fieldConfig.domainFieldId() == null) {
      return Optional.empty();
    }

    List<String> jiraValues = mapDomainsToJiraValues(domains, fieldConfig.domainValues());
    if (jiraValues.isEmpty()) {
      return Optional.empty();
    }

    String fieldKey = toCustomFieldJqlKey(fieldConfig.domainFieldId());
    return Optional.of(inClause(fieldKey, quoteAll(jiraValues)));
  }

  private List<String> mapDomainsToJiraValues(List<String> domains, Map<String, String> domainValues) {
    if (domainValues == null || domainValues.isEmpty()) {
      return new ArrayList<>(domains);
    }
    return domains.stream()
        .map(domain -> domainValues.getOrDefault(domain, domain))
        .toList();
  }

  private String resolveFixVersionField(JiraFieldConfig fieldConfig) {
    if (fieldConfig != null
        && fieldConfig.fixVersionFieldId() != null
        && !fieldConfig.fixVersionFieldId().isBlank()) {
      return toCustomFieldJqlKey(fieldConfig.fixVersionFieldId());
    }
    return "fixVersion";
  }

  private String toCustomFieldJqlKey(String fieldId) {
    if (fieldId.startsWith("customfield_")) {
      return "cf[" + fieldId.substring("customfield_".length()) + "]";
    }
    return fieldId;
  }

  private void addInClause(List<String> clauses, String field, List<String> values) {
    if (values != null && !values.isEmpty()) {
      clauses.add(inClause(field, quoteAll(values)));
    }
  }

  private void addNotInClause(List<String> clauses, String field, List<String> values) {
    if (values != null && !values.isEmpty()) {
      clauses.add(field + " NOT IN (" + String.join(", ", quoteAll(values)) + ")");
    }
  }

  private String inClause(String field, List<String> values) {
    return field + " IN (" + String.join(", ", values) + ")";
  }

  private List<String> quoteAll(List<String> values) {
    if (values == null) {
      return List.of();
    }
    return values.stream()
        .map(this::escapeJqlString)
        .map(value -> "\"" + value + "\"")
        .toList();
  }

  String escapeJqlString(String value) {
    return value.replace("\\", "\\\\").replace("\"", "\\\"");
  }
}
