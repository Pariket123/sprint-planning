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
        List<DomainAllocation> domainAllocations,
        List<DomainAllocation> engineeringAllocations,
        String domainLabel
) {
  public IssueView(
      String key,
      String summary,
      Domain domain,
      Double storyPoints,
      String issueType,
      String status,
      StatusCategory statusCategory) {
    this(key, summary, domain, storyPoints, issueType, status, statusCategory, List.of(), List.of(), null);
  }

  public IssueView(
      String key,
      String summary,
      Domain domain,
      Double storyPoints,
      String issueType,
      String status,
      StatusCategory statusCategory,
      List<DomainAllocation> domainAllocations) {
    this(key, summary, domain, storyPoints, issueType, status, statusCategory, domainAllocations, List.of(), null);
  }

  public IssueView(
      String key,
      String summary,
      Domain domain,
      Double storyPoints,
      String issueType,
      String status,
      StatusCategory statusCategory,
      List<DomainAllocation> domainAllocations,
      List<DomainAllocation> engineeringAllocations) {
    this(key, summary, domain, storyPoints, issueType, status, statusCategory, domainAllocations, engineeringAllocations, null);
  }
}
