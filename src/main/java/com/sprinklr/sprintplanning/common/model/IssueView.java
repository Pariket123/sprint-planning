package com.sprinklr.sprintplanning.common.model;

import com.sprinklr.sprintplanning.common.enums.Domain;
import com.sprinklr.sprintplanning.common.enums.StatusCategory;

import java.util.List;

public record IssueView(
        String key,
        String summary,
        Domain domain,
        Double storyPoints,
        String issueType,
        String status,
        StatusCategory statusCategory,
        List<DomainAllocation> domainAllocations
) {
  public IssueView(
      String key,
      String summary,
      Domain domain,
      Double storyPoints,
      String issueType,
      String status,
      StatusCategory statusCategory) {
    this(key, summary, domain, storyPoints, issueType, status, statusCategory, List.of());
  }
}
