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
    return resolveSectionKey(status, buildStatusToSectionKeyMap(config));
  }

  public Optional<String> resolveSectionKey(String status, Map<String, String> statusToSectionKey) {
    if (status == null || status.isBlank() || statusToSectionKey == null || statusToSectionKey.isEmpty()) {
      return Optional.empty();
    }
    return Optional.ofNullable(statusToSectionKey.get(normalize(status)));
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
