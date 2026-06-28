package com.sprinklr.sprintplanning.common.model;

import java.util.List;
import java.util.Map;

public record DevSubDomainConfig(
    List<String> subDomainPoolSections,
    List<String> autoCompleteSections,
    Map<String, String> completionCheckboxValues
) {
  public boolean hasSubDomainPoolSection(String sectionKey) {
    return sectionKey != null
        && subDomainPoolSections != null
        && subDomainPoolSections.stream().anyMatch(sectionKey::equalsIgnoreCase);
  }

  public boolean autoCompletesInSection(String sectionKey) {
    return sectionKey != null
        && autoCompleteSections != null
        && autoCompleteSections.stream().anyMatch(sectionKey::equalsIgnoreCase);
  }
}
