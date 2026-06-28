package com.sprinklr.sprintplanning.analytics.workflow;

import com.sprinklr.sprintplanning.common.model.WorkflowAnalysisConfig;
import com.sprinklr.sprintplanning.common.model.WorkflowAnalysisSection;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Component
public class WorkflowSectionResolver {

  public Optional<String> resolveSectionKey(String status, WorkflowAnalysisConfig config) {
    if (status == null || status.isBlank() || config == null || config.sections() == null) {
      return Optional.empty();
    }
    String normalizedStatus = normalize(status);
    for (WorkflowAnalysisSection section : config.sections()) {
      if (section.statuses() == null) {
        continue;
      }
      for (String configuredStatus : section.statuses()) {
        if (configuredStatus != null && normalize(configuredStatus).equals(normalizedStatus)) {
          return Optional.of(section.key());
        }
      }
    }
    return Optional.empty();
  }

  public Map<String, String> buildStatusToSectionKeyMap(WorkflowAnalysisConfig config) {
    Map<String, String> lookup = new HashMap<>();
    if (config == null || config.sections() == null) {
      return lookup;
    }
    for (WorkflowAnalysisSection section : config.sections()) {
      if (section.statuses() == null) {
        continue;
      }
      for (String configuredStatus : section.statuses()) {
        if (configuredStatus != null) {
          lookup.put(normalize(configuredStatus), section.key());
        }
      }
    }
    return lookup;
  }

  private String normalize(String value) {
    return value.trim().toLowerCase(Locale.ROOT);
  }
}
