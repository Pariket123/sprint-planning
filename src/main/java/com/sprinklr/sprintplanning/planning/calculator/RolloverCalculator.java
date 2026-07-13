package com.sprinklr.sprintplanning.planning.calculator;

import com.sprinklr.sprintplanning.common.enums.Domain;
import com.sprinklr.sprintplanning.common.model.DomainAllocation;
import com.sprinklr.sprintplanning.common.model.IssueView;
import com.sprinklr.sprintplanning.common.util.IssueAllocationHelper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class RolloverCalculator {

  private final PlanningDomainSupport domainSupport;

  public RolloverCalculator() {
    this.domainSupport = new PlanningDomainSupport();
  }

  public Map<Domain, Double> computeRolloverFromIssues(List<IssueView> previousSprintIssues) {
    Set<Domain> domains = domainSupport.discoverDomainsFromIssues(previousSprintIssues);
    Map<Domain, Double> rollover = domainSupport.initDomainMap(domains);
    for (IssueView issue : domainSupport.nullSafeIssues(previousSprintIssues)) {
      for (DomainAllocation allocation : IssueAllocationHelper.effectiveAllocations(issue)) {
        if (allocation.completed()) {
          continue;
        }
        if (!domainSupport.isPlanningDomain(allocation.domain())) {
          continue;
        }
        rollover.merge(allocation.domain(), allocation.storyPoints(), Double::sum);
      }
    }
    return rollover;
  }

  public Map<Domain, Double> resolveRollover(
      Map<Domain, Double> computedRollover,
      Map<String, Double> manualOverrides) {
    Set<Domain> domains = domainSupport.discoverDomainsFromRollover(computedRollover, manualOverrides);
    Map<Domain, Double> resolved = domainSupport.initDomainMap(domains);
    if (computedRollover != null) {
      computedRollover.forEach((domain, value) -> {
        if (domainSupport.isPlanningDomain(domain) && value != null) {
          resolved.put(domain, value);
        }
      });
    }
    if (manualOverrides == null) {
      return resolved;
    }
    for (Domain domain : domains) {
      Double override = manualOverrides.get(domain.name());
      if (override != null) {
        resolved.put(domain, override);
      }
    }
    return resolved;
  }
}
