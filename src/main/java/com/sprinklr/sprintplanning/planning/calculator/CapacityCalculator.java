package com.sprinklr.sprintplanning.planning.calculator;

import com.sprinklr.sprintplanning.common.enums.Domain;
import com.sprinklr.sprintplanning.common.model.DomainAllocation;
import com.sprinklr.sprintplanning.common.model.IssueView;
import com.sprinklr.sprintplanning.common.util.IssueAllocationHelper;
import com.sprinklr.sprintplanning.planning.model.LeaveEntry;
import com.sprinklr.sprintplanning.planning.model.LeaveType;
import com.sprinklr.sprintplanning.planning.model.PersonCapacity;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class CapacityCalculator {

  private final PlanningDomainSupport domainSupport;

  public CapacityCalculator() {
    this.domainSupport = new PlanningDomainSupport();
  }

  public Set<Domain> discoverPlanningDomains(PlanningCalculationInput input) {
    return domainSupport.discoverPlanningDomains(input);
  }

  public Map<Domain, Double> calculateAvailableCapacity(
      List<PersonCapacity> capacity,
      List<LeaveEntry> leaves,
      LocalDate sprintStart,
      LocalDate sprintEnd,
      int workingDays,
      Set<Domain> planningDomains) {
    Map<Domain, Double> available = domainSupport.initDomainMap(planningDomains);

    if (capacity != null) {
      for (PersonCapacity entry : capacity) {
        if (entry == null || !domainSupport.isPlanningDomain(entry.getDomain())) {
          continue;
        }
        if (!planningDomains.contains(entry.getDomain())) {
          planningDomains.add(entry.getDomain());
          available.put(entry.getDomain(), 0.0);
        }
        double memberDays = (entry.getBandwidthPercent() / 100.0) * workingDays * entry.getVelocity();
        available.merge(entry.getDomain(), memberDays, Double::sum);
      }
    }

    if (leaves != null) {
      for (LeaveEntry leave : leaves) {
        if (leave == null || leave.getType() != LeaveType.LEAVE) {
          continue;
        }
        int leaveDays = countOverlappingBusinessDays(
            leave.getStartDate(), leave.getEndDate(), sprintStart, sprintEnd);
        if (leaveDays <= 0) {
          continue;
        }
        double bandwidthPercent = resolveLeaveBandwidth(leave, capacity);
        double velocity = resolveLeaveVelocity(leave, capacity);
        double deduction = leaveDays * (bandwidthPercent / 100.0) * velocity;
        if (leave.getDomain() == null) {
          for (Domain domain : planningDomains) {
            available.put(domain, available.get(domain) - deduction);
          }
        } else if (domainSupport.isPlanningDomain(leave.getDomain())) {
          available.merge(leave.getDomain(), -deduction, Double::sum);
        }
      }
    }

    for (Domain domain : planningDomains) {
      available.put(domain, Math.max(0, available.getOrDefault(domain, 0.0)));
    }
    return available;
  }

  public Map<Domain, SelectionMetrics> calculateIssueMetrics(List<IssueView> issues, Set<Domain> planningDomains) {
    Map<Domain, SelectionMetrics> metrics = new EnumMap<>(Domain.class);
    for (Domain domain : planningDomains) {
      metrics.put(domain, new SelectionMetrics());
    }
    for (IssueView issue : domainSupport.nullSafeIssues(issues)) {
      if (issue == null) {
        continue;
      }
      Map<Domain, Double> storyPointsByDomain = new EnumMap<>(Domain.class);
      for (DomainAllocation allocation : IssueAllocationHelper.effectiveAllocations(issue)) {
        if (!domainSupport.isPlanningDomain(allocation.domain())) {
          continue;
        }
        storyPointsByDomain.merge(allocation.domain(), allocation.storyPoints(), Double::sum);
      }
      String issueKey = issue.key();
      for (Map.Entry<Domain, Double> entry : storyPointsByDomain.entrySet()) {
        SelectionMetrics domainMetrics = metrics.computeIfAbsent(entry.getKey(), ignored -> new SelectionMetrics());
        domainMetrics.addStoryPoints(entry.getValue());
        domainMetrics.recordUniqueIssue(issueKey);
      }
    }
    return metrics;
  }

  public int countHolidayDays(List<LeaveEntry> leaves, LocalDate sprintStart, LocalDate sprintEnd) {
    if (leaves == null) {
      return 0;
    }
    int holidayDays = 0;
    for (LeaveEntry leave : leaves) {
      if (leave != null && leave.getType() == LeaveType.HOLIDAY) {
        holidayDays += countOverlappingBusinessDays(leave.getStartDate(), leave.getEndDate(), sprintStart, sprintEnd);
      }
    }
    return holidayDays;
  }

  public int countBusinessDays(LocalDate start, LocalDate end) {
    if (start == null || end == null || end.isBefore(start)) {
      return 0;
    }
    int days = 0;
    for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
      if (isBusinessDay(date)) {
        days++;
      }
    }
    return days;
  }

  public LocalDate toLocalDate(Instant instant) {
    return instant != null ? instant.atZone(ZoneOffset.UTC).toLocalDate() : null;
  }

  public LocalDate resolveReleaseEnd(LocalDate start, int durationDays) {
    if (start == null || durationDays <= 0) {
      return null;
    }
    int counted = 0;
    LocalDate date = start;
    while (true) {
      if (isBusinessDay(date)) {
        counted++;
        if (counted >= durationDays) {
          return date;
        }
      }
      date = date.plusDays(1);
    }
  }

  private int countOverlappingBusinessDays(
      LocalDate leaveStart, LocalDate leaveEnd, LocalDate sprintStart, LocalDate sprintEnd) {
    if (leaveStart == null || leaveEnd == null || sprintStart == null || sprintEnd == null) {
      return 0;
    }
    LocalDate start = leaveStart.isBefore(sprintStart) ? sprintStart : leaveStart;
    LocalDate end = leaveEnd.isAfter(sprintEnd) ? sprintEnd : leaveEnd;
    return countBusinessDays(start, end);
  }

  private boolean isBusinessDay(LocalDate date) {
    DayOfWeek day = date.getDayOfWeek();
    return day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY;
  }

  private double resolveLeaveBandwidth(LeaveEntry leave, List<PersonCapacity> capacity) {
    if (leave.getPersonName() != null && !leave.getPersonName().isBlank() && capacity != null) {
      for (PersonCapacity person : capacity) {
        if (person != null
            && leave.getPersonName().equalsIgnoreCase(person.getPersonName())) {
          return person.getBandwidthPercent();
        }
      }
    }
    return 100.0;
  }

  private double resolveLeaveVelocity(LeaveEntry leave, List<PersonCapacity> capacity) {
    if (leave.getPersonName() != null && !leave.getPersonName().isBlank() && capacity != null) {
      for (PersonCapacity person : capacity) {
        if (person != null
            && leave.getPersonName().equalsIgnoreCase(person.getPersonName())) {
          return person.getVelocity();
        }
      }
    }
    return 1.0;
  }
}
