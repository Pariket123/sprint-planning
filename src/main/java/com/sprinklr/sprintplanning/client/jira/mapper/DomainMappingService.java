package com.sprinklr.sprintplanning.client.jira.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.sprinklr.sprintplanning.client.jira.dto.JiraIssueDto;
import com.sprinklr.sprintplanning.common.enums.Domain;
import com.sprinklr.sprintplanning.common.model.DomainAllocation;
import com.sprinklr.sprintplanning.common.model.JiraFieldConfig;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class DomainMappingService {

  private static final List<Domain> STAGE_DOMAINS = List.of(Domain.DEV, Domain.QA, Domain.DESIGN);
  private static final Set<Domain> ENGINEERING_DOMAINS = EnumSet.of(Domain.BE, Domain.UI, Domain.AI);

  private final JiraFieldReader fieldReader;

  public DomainMappingService(JiraFieldReader fieldReader) {
    this.fieldReader = fieldReader;
  }

  public List<DomainAllocation> resolveDomainAllocations(JiraIssueDto issue, JiraFieldConfig config) {
    if (config == null || !config.hasMultiDomainSupport()) {
      return List.of();
    }
    JsonNode fields = issue.getFields();
    if (fields == null) {
      return List.of();
    }

    Set<String> completedOptions = fieldReader.readMultiCheckboxOptions(
        fields.path(config.domainCompletionFieldId()));

    List<DomainAllocation> allocations = new ArrayList<>();
    allocations.addAll(resolveEngineeringDomainAllocations(fields, completedOptions, config));
    allocations.addAll(resolveStageDomainAllocations(fields, completedOptions, config));
    return allocations;
  }

  public List<DomainAllocation> resolveEngineeringAllocations(JiraIssueDto issue, JiraFieldConfig config) {
    if (config == null || config.domainStoryPointFields() == null || config.domainStoryPointFields().isEmpty()) {
      return List.of();
    }
    JsonNode fields = issue.getFields();
    if (fields == null) {
      return List.of();
    }

    Set<String> completedOptions = fieldReader.readMultiCheckboxOptions(
        fields.path(config.domainCompletionFieldId()));
    return resolveEngineeringDomainAllocations(fields, completedOptions, config);
  }

  public Domain resolveDomain(JiraIssueDto issue, JiraFieldConfig config) {
    List<DomainAllocation> allocations = resolveDomainAllocations(issue, config);
    if (!allocations.isEmpty()) {
      return allocations.stream()
          .map(DomainAllocation::domain)
          .filter(ENGINEERING_DOMAINS::contains)
          .findFirst()
          .orElse(allocations.getFirst().domain());
    }

    if (config == null || config.domainFieldId() == null) {
      return Domain.UNKNOWN;
    }
    JsonNode fields = issue.getFields();
    if (fields == null) {
      return Domain.UNKNOWN;
    }
    String jiraValue = fieldReader.extractFieldValue(fields.path(config.domainFieldId()));
    List<Domain> domains = parseDomainsFromSelect(jiraValue, config);
    return domains.isEmpty() ? Domain.UNKNOWN : domains.getFirst();
  }

  public String resolveDomainLabel(JiraIssueDto issue, JiraFieldConfig config) {
    if (config == null || config.domainFieldId() == null) {
      Domain domain = resolveDomain(issue, config);
      return domain == Domain.UNKNOWN ? null : domain.name();
    }
    JsonNode fields = issue.getFields();
    if (fields == null) {
      return null;
    }
    String jiraValue = fieldReader.extractFieldValue(fields.path(config.domainFieldId()));
    return formatDomainLabel(jiraValue, config);
  }

  public String formatDomainLabel(String jiraValue, JiraFieldConfig config) {
    if (jiraValue == null || jiraValue.isBlank()) {
      return null;
    }

    if (jiraValue.contains("+")) {
      List<String> labels = new ArrayList<>();
      for (String token : jiraValue.split("\\+")) {
        mapTokenToDomainKey(token.trim(), config).ifPresent(labels::add);
      }
      if (!labels.isEmpty()) {
        return String.join(" ", labels);
      }
    }

    return mapTokenToDomainKey(jiraValue.trim(), config).orElse(jiraValue.trim());
  }

  public List<Domain> parseDomainsFromSelect(String jiraValue, JiraFieldConfig config) {
    if (jiraValue == null || jiraValue.isBlank() || config == null) {
      return List.of(Domain.UNKNOWN);
    }

    if (config.compositeDomainValues() != null) {
      for (Map.Entry<String, String> entry : config.compositeDomainValues().entrySet()) {
        if (jiraValue.equalsIgnoreCase(entry.getValue())) {
          return parseDomainKeys(entry.getKey());
        }
      }
    }

    if (config.domainValues() != null) {
      for (Map.Entry<String, String> entry : config.domainValues().entrySet()) {
        if (jiraValue.equalsIgnoreCase(entry.getValue())) {
          return domainsFromKey(entry.getKey());
        }
      }
    }

    if (jiraValue.contains("+")) {
      List<Domain> domains = new ArrayList<>();
      for (String token : jiraValue.split("\\+")) {
        mapTokenToDomain(token.trim(), config).ifPresent(domains::add);
      }
      if (!domains.isEmpty()) {
        return domains;
      }
    }

    return mapTokenToDomain(jiraValue, config).map(List::of).orElse(List.of(Domain.UNKNOWN));
  }

  private List<DomainAllocation> resolveEngineeringDomainAllocations(
      JsonNode fields,
      Set<String> completedOptions,
      JiraFieldConfig config) {
    String jiraDomainValue = config.domainFieldId() != null
        ? fieldReader.extractFieldValue(fields.path(config.domainFieldId()))
        : null;
    List<Domain> selectedDomains = parseDomainsFromSelect(jiraDomainValue, config).stream()
        .filter(ENGINEERING_DOMAINS::contains)
        .distinct()
        .toList();

    if (selectedDomains.isEmpty()) {
      selectedDomains = ENGINEERING_DOMAINS.stream()
          .filter(domain -> hasPositiveStoryPoints(fields, domain, config))
          .toList();
    }

    List<DomainAllocation> allocations = new ArrayList<>();
    for (Domain domain : selectedDomains) {
      if (!usesPerDomainStoryPoints(domain, config)) {
        continue;
      }
      double storyPoints = fieldReader.numberOrZero(
          fieldReader.readNumberField(fields, config.domainStoryPointFields().get(domain.name())));
      boolean completed = isDomainCompleted(domain, completedOptions, config);
      allocations.add(new DomainAllocation(domain, storyPoints, completed));
    }
    return allocations;
  }

  private List<DomainAllocation> resolveStageDomainAllocations(
      JsonNode fields,
      Set<String> completedOptions,
      JiraFieldConfig config) {
    List<DomainAllocation> allocations = new ArrayList<>();
    for (Domain domain : STAGE_DOMAINS) {
      if (!usesPerDomainStoryPoints(domain, config)) {
        continue;
      }
      double storyPoints = resolveAllocationStoryPoints(fields, domain, config);
      if (storyPoints <= 0) {
        continue;
      }
      boolean completed = isDomainCompleted(domain, completedOptions, config);
      allocations.add(new DomainAllocation(domain, storyPoints, completed));
    }
    return allocations;
  }

  private boolean hasPositiveStoryPoints(JsonNode fields, Domain domain, JiraFieldConfig config) {
    String storyPointFieldId = config.domainStoryPointFields().get(domain.name());
    if (storyPointFieldId == null || storyPointFieldId.isBlank()) {
      return false;
    }
    return fieldReader.numberOrZero(fieldReader.readNumberField(fields, storyPointFieldId)) > 0;
  }

  private double resolveAllocationStoryPoints(JsonNode fields, Domain domain, JiraFieldConfig config) {
    if (usesPerDomainStoryPoints(domain, config)) {
      String storyPointFieldId = config.domainStoryPointFields().get(domain.name());
      return fieldReader.numberOrZero(fieldReader.readNumberField(fields, storyPointFieldId));
    }
    return fieldReader.numberOrZero(fieldReader.readNumberField(fields, config.storyPointsFieldId()));
  }

  private boolean usesPerDomainStoryPoints(Domain domain, JiraFieldConfig config) {
    if (domain == null || config == null || config.domainStoryPointFields() == null) {
      return false;
    }
    String storyPointFieldId = config.domainStoryPointFields().get(domain.name());
    return storyPointFieldId != null && !storyPointFieldId.isBlank();
  }

  private List<Domain> parseDomainKeys(String compositeKey) {
    if (compositeKey == null || compositeKey.isBlank()) {
      return List.of(Domain.UNKNOWN);
    }
    List<Domain> domains = new ArrayList<>();
    for (String token : compositeKey.split("\\+")) {
      resolveDomainEnum(token.trim()).ifPresent(domains::add);
    }
    return domains.isEmpty() ? List.of(Domain.UNKNOWN) : domains;
  }

  private List<Domain> domainsFromKey(String domainKey) {
    return resolveDomainEnum(domainKey).map(List::of).orElse(List.of(Domain.UNKNOWN));
  }

  private Optional<Domain> resolveDomainEnum(String domainKey) {
    if (domainKey == null || domainKey.isBlank()) {
      return Optional.empty();
    }
    try {
      return Optional.of(Domain.valueOf(domainKey.trim()));
    } catch (IllegalArgumentException ex) {
      return Optional.empty();
    }
  }

  private Optional<Domain> mapTokenToDomain(String token, JiraFieldConfig config) {
    if (token == null || token.isBlank()) {
      return Optional.empty();
    }
    Optional<Domain> direct = resolveDomainEnum(token);
    if (direct.isPresent()) {
      return direct;
    }
    if (config.domainValues() != null) {
      for (Map.Entry<String, String> entry : config.domainValues().entrySet()) {
        if (token.equalsIgnoreCase(entry.getValue()) || token.equalsIgnoreCase(entry.getKey())) {
          return resolveDomainEnum(entry.getKey());
        }
      }
    }
    return Optional.empty();
  }

  private Optional<String> mapTokenToDomainKey(String token, JiraFieldConfig config) {
    return mapTokenToDomain(token, config).map(Domain::name);
  }

  private boolean isDomainCompleted(Domain domain, Set<String> completedOptions, JiraFieldConfig config) {
    if (config.domainCompletionValues() == null || completedOptions.isEmpty()) {
      return false;
    }
    String optionLabel = config.domainCompletionValues().get(domain.name());
    if (optionLabel == null) {
      return false;
    }
    return completedOptions.stream().anyMatch(option -> optionLabel.equalsIgnoreCase(option));
  }
}
