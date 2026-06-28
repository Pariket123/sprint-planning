package com.sprinklr.sprintplanning.common.model;

import java.util.List;

public record WorkflowAnalysisConfig(
    List<WorkflowAnalysisSection> sections,
    DevSubDomainConfig devSubDomain
) {
  public boolean isConfigured() {
    return sections != null && !sections.isEmpty();
  }
}
