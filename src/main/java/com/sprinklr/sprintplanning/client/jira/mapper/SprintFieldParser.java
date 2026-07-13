package com.sprinklr.sprintplanning.client.jira.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.sprinklr.sprintplanning.client.jira.dto.JiraIssueDto;
import com.sprinklr.sprintplanning.common.model.JiraFieldConfig;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SprintFieldParser {

  private static final Pattern SPRINT_ID_IN_TEXT = Pattern.compile("id=(\\d+)");
  private static final Pattern SPRINT_STATE_IN_TEXT =
      Pattern.compile("state=([A-Za-z_]+)", Pattern.CASE_INSENSITIVE);

  private final JiraFieldReader fieldReader;

  public SprintFieldParser(JiraFieldReader fieldReader) {
    this.fieldReader = fieldReader;
  }

  public List<Long> resolveSprintIds(JsonNode fields, JiraFieldConfig config) {
    if (fields.isMissingNode()) {
      return List.of();
    }
    JsonNode sprintField = sprintFieldNode(fields, config);
    List<SprintRef> refs = !sprintField.isMissingNode() && !sprintField.isNull()
        ? parseSprintRefs(sprintField)
        : findSprintRefsInAllFields(fields);
    return orderSprintIdsWithCurrentLast(refs);
  }

  public Long resolveCurrentSprintId(JsonNode fields, JiraFieldConfig config) {
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
      String state = fieldReader.textOrNull(item.path("state"));
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

  record SprintRef(long id, String state) {}
}
