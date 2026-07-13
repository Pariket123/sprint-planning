package com.sprinklr.sprintplanning.planning.calculator;

import com.sprinklr.sprintplanning.common.enums.Domain;
import com.sprinklr.sprintplanning.common.model.DomainAllocation;
import com.sprinklr.sprintplanning.common.model.IssueView;
import com.sprinklr.sprintplanning.common.util.IssueAllocationHelper;
import com.sprinklr.sprintplanning.planning.model.LeaveEntry;
import com.sprinklr.sprintplanning.planning.model.PersonCapacity;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

final class PlanningDomainSupport {

  static final Set<Domain> DEFAULT_DOMAINS = EnumSet.of(Domain.DEV, Domain.QA, Domain.DESIGN);

  boolean isPlanningDomain(Domain domain) {
    return domain != null && domain != Domain.UNKNOWN;
  }

  Map<Domain, Double> initDomainMap(Set<Domain> domains) {
    Map<Domain, Double> map = new EnumMap<>(Domain.class);
    for (Domain domain : domains) {
      map.put(domain, 0.0);
    }
    return map;
  }

  Set<Domain> discoverPlanningDomains(PlanningCalculationInput input) {
    Set<Domain> domains = new LinkedHashSet<>();
    addDomainsFromCapacity(domains, input.capacity());
    addDomainsFromLeaves(domains, input.leaves());
    addDomainsFromIssues(domains, input.selectedIssues());
    addDomainsFromIssues(domains, input.committedIssues());
    addDomainsFromRollover(domains, input.computedRollover(), input.manualRolloverOverrides());
    if (domains.isEmpty()) {
      domains.addAll(DEFAULT_DOMAINS);
    }
    return domains;
  }

  Set<Domain> discoverDomainsFromIssues(List<IssueView> issues) {
    Set<Domain> domains = new LinkedHashSet<>();
    addDomainsFromIssues(domains, issues);
    if (domains.isEmpty()) {
      domains.addAll(DEFAULT_DOMAINS);
    }
    return domains;
  }

  Set<Domain> discoverDomainsFromRollover(
      Map<Domain, Double> computedRollover,
      Map<String, Double> manualOverrides) {
    Set<Domain> domains = new LinkedHashSet<>();
    addDomainsFromRollover(domains, computedRollover, manualOverrides);
    if (domains.isEmpty()) {
      domains.addAll(DEFAULT_DOMAINS);
    }
    return domains;
  }

  void addDomainsFromCapacity(Set<Domain> domains, List<PersonCapacity> capacity) {
    if (capacity == null) {
      return;
    }
    for (PersonCapacity entry : capacity) {
      if (entry != null && isPlanningDomain(entry.getDomain())) {
        domains.add(entry.getDomain());
      }
    }
  }

  void addDomainsFromLeaves(Set<Domain> domains, List<LeaveEntry> leaves) {
    if (leaves == null) {
      return;
    }
    for (LeaveEntry leave : leaves) {
      if (leave != null && isPlanningDomain(leave.getDomain())) {
        domains.add(leave.getDomain());
      }
    }
  }

  void addDomainsFromIssues(Set<Domain> domains, List<IssueView> issues) {
    for (IssueView issue : nullSafeIssues(issues)) {
      for (DomainAllocation allocation : IssueAllocationHelper.effectiveAllocations(issue)) {
        if (isPlanningDomain(allocation.domain())) {
          domains.add(allocation.domain());
        }
      }
    }
  }

  void addDomainsFromRollover(
      Set<Domain> domains,
      Map<Domain, Double> computedRollover,
      Map<String, Double> manualOverrides) {
    if (computedRollover != null) {
      computedRollover.keySet().stream()
          .filter(this::isPlanningDomain)
          .forEach(domains::add);
    }
    if (manualOverrides != null) {
      for (String domainName : manualOverrides.keySet()) {
        parseDomain(domainName).ifPresent(domains::add);
      }
    }
  }

  Optional<Domain> parseDomain(String domainName) {
    if (domainName == null || domainName.isBlank()) {
      return Optional.empty();
    }
    try {
      Domain domain = Domain.valueOf(domainName.trim());
      return isPlanningDomain(domain) ? Optional.of(domain) : Optional.empty();
    } catch (IllegalArgumentException ex) {
      return Optional.empty();
    }
  }

  List<IssueView> nullSafeIssues(List<IssueView> issues) {
    return issues != null ? issues : List.of();
  }
}
