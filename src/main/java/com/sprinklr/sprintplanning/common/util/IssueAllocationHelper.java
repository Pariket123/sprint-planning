package com.sprinklr.sprintplanning.common.util;

import com.sprinklr.sprintplanning.common.enums.Domain;
import com.sprinklr.sprintplanning.common.enums.StatusCategory;
import com.sprinklr.sprintplanning.common.model.DomainAllocation;
import com.sprinklr.sprintplanning.common.model.IssueView;

import java.util.List;

public final class IssueAllocationHelper {

  private IssueAllocationHelper() {
  }

  public static List<DomainAllocation> effectiveAllocations(IssueView issue) {
    if (issue == null) {
      return List.of();
    }
    if (issue.domainAllocations() != null && !issue.domainAllocations().isEmpty()) {
      return issue.domainAllocations();
    }
    Domain domain = issue.domain() != null ? issue.domain() : Domain.UNKNOWN;
    double storyPoints = issue.storyPoints() != null ? issue.storyPoints() : 0.0;
    boolean completed = issue.statusCategory() == StatusCategory.DONE;
    return List.of(new DomainAllocation(domain, storyPoints, completed));
  }

  public static double totalStoryPoints(IssueView issue) {
    return effectiveAllocations(issue).stream()
        .mapToDouble(DomainAllocation::storyPoints)
        .sum();
  }

  public static boolean isIssueCompleted(IssueView issue) {
    return issue != null && issue.statusCategory() == StatusCategory.DONE;
  }

  public static boolean isAllocationCompleted(IssueView issue, DomainAllocation allocation) {
    if (allocation.completed()) {
      return true;
    }
    return isIssueCompleted(issue);
  }

  public static double completedStoryPoints(IssueView issue) {
    return effectiveAllocations(issue).stream()
        .filter(allocation -> isAllocationCompleted(issue, allocation))
        .mapToDouble(DomainAllocation::storyPoints)
        .sum();
  }

  public static boolean isFullyCompleted(IssueView issue) {
    List<DomainAllocation> allocations = effectiveAllocations(issue);
    if (allocations.isEmpty()) {
      return false;
    }
    return allocations.stream().allMatch(DomainAllocation::completed);
  }
}
