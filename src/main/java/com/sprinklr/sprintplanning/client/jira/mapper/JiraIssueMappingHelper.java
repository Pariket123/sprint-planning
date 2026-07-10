package com.sprinklr.sprintplanning.client.jira.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.sprinklr.sprintplanning.client.jira.dto.JiraIssueDto;
import com.sprinklr.sprintplanning.common.enums.Domain;
import com.sprinklr.sprintplanning.common.enums.StatusCategory;
import com.sprinklr.sprintplanning.common.model.DomainAllocation;
import com.sprinklr.sprintplanning.common.model.JiraFieldConfig;
import org.mapstruct.Context;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class JiraIssueMappingHelper {

  private static final List<Domain> STAGE_DOMAINS = List.of(Domain.DEV, Domain.QA, Domain.DESIGN);
  private static final Set<Domain> ENGINEERING_DOMAINS = EnumSet.of(Domain.BE, Domain.UI, Domain.AI);

  private static final Pattern SPRINT_ID_IN_TEXT = Pattern.compile("id=(\\d+)");
  private static final Pattern SPRINT_STATE_IN_TEXT =
      Pattern.compile("state=([A-Za-z_]+)", Pattern.CASE_INSENSITIVE);

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
    List<DomainAllocation> allocations = resolveDomainAllocations(issue, config);
    if (!allocations.isEmpty()) {
      return allocations.stream().mapToDouble(DomainAllocation::storyPoints).sum();
    }
    return readNumberField(fieldsOrNull(issue), config != null ? config.storyPointsFieldId() : null);
  }

  @Named("resolveDomainAllocations")
  public List<DomainAllocation> resolveDomainAllocations(JiraIssueDto issue, @Context JiraFieldConfig config) {
    if (config == null || !config.hasMultiDomainSupport()) {
      return List.of();
    }
    JsonNode fields = issue.getFields();
    if (fields == null) {
      return List.of();
    }

    Set<String> completedOptions = readMultiCheckboxOptions(
        fields.path(config.domainCompletionFieldId()));

    List<DomainAllocation> allocations = new ArrayList<>();
    allocations.addAll(resolveEngineeringDomainAllocations(fields, completedOptions, config));
    allocations.addAll(resolveStageDomainAllocations(fields, completedOptions, config));
    return allocations;
  }

  @Named("resolveEngineeringAllocations")
  public List<DomainAllocation> resolveEngineeringAllocations(JiraIssueDto issue, @Context JiraFieldConfig config) {
    if (config == null || config.domainStoryPointFields() == null || config.domainStoryPointFields().isEmpty()) {
      return List.of();
    }
    JsonNode fields = issue.getFields();
    if (fields == null) {
      return List.of();
    }

    Set<String> completedOptions = readMultiCheckboxOptions(
        fields.path(config.domainCompletionFieldId()));
    return resolveEngineeringDomainAllocations(fields, completedOptions, config);
  }

  private List<DomainAllocation> resolveEngineeringDomainAllocations(
      JsonNode fields,
      Set<String> completedOptions,
      JiraFieldConfig config) {
    String jiraDomainValue = config.domainFieldId() != null
        ? extractFieldValue(fields.path(config.domainFieldId()))
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
      double storyPoints = numberOrZero(readNumberField(fields, config.domainStoryPointFields().get(domain.name())));
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
    return numberOrZero(readNumberField(fields, storyPointFieldId)) > 0;
  }

  private double resolveAllocationStoryPoints(JsonNode fields, Domain domain, JiraFieldConfig config) {
    if (usesPerDomainStoryPoints(domain, config)) {
      String storyPointFieldId = config.domainStoryPointFields().get(domain.name());
      return numberOrZero(readNumberField(fields, storyPointFieldId));
    }
    return numberOrZero(readNumberField(fields, config.storyPointsFieldId()));
  }

  private boolean usesPerDomainStoryPoints(Domain domain, JiraFieldConfig config) {
    if (domain == null || config == null || config.domainStoryPointFields() == null) {
      return false;
    }
    String storyPointFieldId = config.domainStoryPointFields().get(domain.name());
    return storyPointFieldId != null && !storyPointFieldId.isBlank();
  }

  @Named("resolveDomain")
  public Domain resolveDomain(JiraIssueDto issue, @Context JiraFieldConfig config) {
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
    String jiraValue = extractFieldValue(fields.path(config.domainFieldId()));
    List<Domain> domains = parseDomainsFromSelect(jiraValue, config);
    return domains.isEmpty() ? Domain.UNKNOWN : domains.getFirst();
  }

  @Named("resolveDomainLabel")
  public String resolveDomainLabel(JiraIssueDto issue, @Context JiraFieldConfig config) {
    if (config == null || config.domainFieldId() == null) {
      Domain domain = resolveDomain(issue, config);
      return domain == Domain.UNKNOWN ? null : domain.name();
    }
    JsonNode fields = issue.getFields();
    if (fields == null) {
      return null;
    }
    String jiraValue = extractFieldValue(fields.path(config.domainFieldId()));
    return formatDomainLabel(jiraValue, config);
  }

  String formatDomainLabel(String jiraValue, JiraFieldConfig config) {
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

  private Optional<String> mapTokenToDomainKey(String token, JiraFieldConfig config) {
    return mapTokenToDomain(token, config).map(Domain::name);
  }

  List<Domain> parseDomainsFromSelect(String jiraValue, JiraFieldConfig config) {
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

  private Set<String> readMultiCheckboxOptions(JsonNode node) {
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

  private Double readNumberField(JsonNode fields, String fieldId) {
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

  private double numberOrZero(Double value) {
    return value != null ? value : 0.0;
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
  public List<String> resolveFixVersions(JiraIssueDto issue, @Context JiraFieldConfig config) {
    List<String> builtInVersions = readBuiltInFixVersions(issue);
    if (!builtInVersions.isEmpty()) {
      return builtInVersions;
    }

    if (config != null && config.fixVersionFieldId() != null && !config.fixVersionFieldId().isBlank()) {
      String customFixVersion = textOrNull(fieldsOrNull(issue).path(config.fixVersionFieldId()));
      if (customFixVersion == null || customFixVersion.isBlank()) {
        return List.of();
      }
      return List.of(customFixVersion.trim());
    }

    return List.of();
  }

  private List<String> readBuiltInFixVersions(JiraIssueDto issue) {
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
  public List<Long> resolveSprintIds(JiraIssueDto issue, @Context JiraFieldConfig config) {
    JsonNode fields = fieldsOrNull(issue);
    if (fields.isMissingNode()) {
      return List.of();
    }
    JsonNode sprintField = sprintFieldNode(fields, config);
    List<SprintRef> refs = !sprintField.isMissingNode() && !sprintField.isNull()
        ? parseSprintRefs(sprintField)
        : findSprintRefsInAllFields(fields);
    return orderSprintIdsWithCurrentLast(refs);
  }

  /**
   * Resolves the issue's current sprint from the configured sprint field, preferring active/future
   * sprints over closed ones when Jira returns sprint history. Returns null when the issue is on
   * the backlog (no active/future sprint), even if closed sprint history remains in the field.
   */
  @Named("resolveCurrentSprintId")
  public Long resolveCurrentSprintId(JiraIssueDto issue, @Context JiraFieldConfig config) {
    JsonNode fields = fieldsOrNull(issue);
    if (fields.isMissingNode()) {
      return null;
    }
    JsonNode sprintField = sprintFieldNode(fields, config);
    List<SprintRef> refs = !sprintField.isMissingNode() && !sprintField.isNull()
        ? parseSprintRefs(sprintField)
        : findSprintRefsInAllFields(fields);
    return resolvePreferredSprintIdFromRefs(refs).orElse(null);
  }

  private JsonNode sprintFieldNode(JsonNode fields, JiraFieldConfig config) {
    if (config != null && config.sprintFieldId() != null && !config.sprintFieldId().isBlank()) {
      return fields.path(config.sprintFieldId());
    }
    return JsonNodeFactory.instance.missingNode();
  }

  private List<SprintRef> findSprintRefsInAllFields(JsonNode fields) {
    List<SprintRef> refs = new ArrayList<>();
    Iterator<Map.Entry<String, JsonNode>> fieldIterator = fields.fields();
    while (fieldIterator.hasNext()) {
      refs.addAll(parseSprintRefs(fieldIterator.next().getValue()));
    }
    return refs;
  }

  private List<Long> orderSprintIdsWithCurrentLast(List<SprintRef> refs) {
    if (refs.isEmpty()) {
      return List.of();
    }
    Long preferredId = resolvePreferredSprintIdFromRefs(refs).orElse(refs.getLast().id());
    LinkedHashSet<Long> ordered = new LinkedHashSet<>();
    for (SprintRef ref : refs) {
      if (ref.id() != preferredId) {
        ordered.add(ref.id());
      }
    }
    ordered.add(preferredId);
    return new ArrayList<>(ordered);
  }

  private Optional<Long> resolvePreferredSprintIdFromRefs(List<SprintRef> refs) {
    for (String preferredState : List.of("active", "future")) {
      for (int index = refs.size() - 1; index >= 0; index--) {
        SprintRef ref = refs.get(index);
        if (preferredState.equalsIgnoreCase(ref.state())) {
          return Optional.of(ref.id());
        }
      }
    }
    return Optional.empty();
  }

  private Optional<Long> resolvePreferredSprintId(JsonNode sprintField) {
    return resolvePreferredSprintIdFromRefs(parseSprintRefs(sprintField));
  }

  private List<SprintRef> parseSprintRefs(JsonNode node) {
    if (!node.isArray()) {
      return List.of();
    }
    List<SprintRef> refs = new ArrayList<>();
    for (JsonNode item : node) {
      parseSprintRef(item).ifPresent(refs::add);
    }
    return refs;
  }

  private Optional<SprintRef> parseSprintRef(JsonNode item) {
    if (item.isObject() && item.has("id")) {
      JsonNode idNode = item.get("id");
      Long id = idNode.isNumber() ? idNode.asLong() : parseLong(idNode.asText());
      if (id == null) {
        return Optional.empty();
      }
      String state = textOrNull(item.path("state"));
      return Optional.of(new SprintRef(id, state != null ? state : "unknown"));
    }
    if (item.isTextual()) {
      return parseSprintRefFromText(item.asText());
    }
    return Optional.empty();
  }

  private Optional<SprintRef> parseSprintRefFromText(String text) {
    Matcher idMatcher = SPRINT_ID_IN_TEXT.matcher(text);
    if (!idMatcher.find()) {
      return Optional.empty();
    }
    Long id = parseLong(idMatcher.group(1));
    if (id == null) {
      return Optional.empty();
    }
    Matcher stateMatcher = SPRINT_STATE_IN_TEXT.matcher(text);
    String state = stateMatcher.find() ? stateMatcher.group(1) : "unknown";
    return Optional.of(new SprintRef(id, state));
  }

  private List<Long> parseSprintFieldNode(JsonNode node) {
    return orderSprintIdsWithCurrentLast(parseSprintRefs(node));
  }

  private Long parseLong(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException ex) {
      return null;
    }
  }

  private record SprintRef(long id, String state) {}

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
