package com.sprinklr.sprintplanning.analytics.calculator;

import com.sprinklr.sprintplanning.analytics.dto.DevSubDomainMetricsDto;
import com.sprinklr.sprintplanning.analytics.dto.WorkflowStageDistributionDto;
import com.sprinklr.sprintplanning.analytics.workflow.WorkflowAnalyticsCalculator;
import com.sprinklr.sprintplanning.analytics.dto.AnalyticsResponse;
import com.sprinklr.sprintplanning.analytics.dto.BugsVsFeaturesDto;
import com.sprinklr.sprintplanning.analytics.dto.CategoryMetricsDto;
import com.sprinklr.sprintplanning.analytics.dto.DomainBreakdownItemDto;
import com.sprinklr.sprintplanning.analytics.dto.IssueCountsDto;
import com.sprinklr.sprintplanning.analytics.dto.IssueTypeMetricsDto;
import com.sprinklr.sprintplanning.analytics.dto.StatusDistributionItemDto;
import com.sprinklr.sprintplanning.analytics.workflow.DevSubDomainAnalysisProfiles;
import com.sprinklr.sprintplanning.common.enums.Domain;
import com.sprinklr.sprintplanning.common.enums.StatusCategory;
import com.sprinklr.sprintplanning.common.model.DomainAllocation;
import com.sprinklr.sprintplanning.common.model.IssueView;
import com.sprinklr.sprintplanning.common.model.JiraFieldConfig;
import com.sprinklr.sprintplanning.common.util.IssueAllocationHelper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class AnalyticsCalculator {

  private final WorkflowAnalyticsCalculator workflowAnalyticsCalculator;

  public AnalyticsCalculator(WorkflowAnalyticsCalculator workflowAnalyticsCalculator) {
    this.workflowAnalyticsCalculator = workflowAnalyticsCalculator;
  }

  public AnalyticsResponse calculate(Long jiraSprintId, String sprintName, List<IssueView> issues,
                                     JiraFieldConfig fieldConfig) {
    return calculate(jiraSprintId, sprintName, issues, issues, fieldConfig);
  }

  public AnalyticsResponse calculate(
      Long jiraSprintId,
      String sprintName,
      List<IssueView> issues,
      List<IssueView> issueTypeBreakdownIssues,
      JiraFieldConfig fieldConfig) {
    double totalStoryPoints = 0;
    double completedStoryPoints = 0;
    int completedCount = 0;

    Map<String, StatusBucket> statusBuckets = new LinkedHashMap<>();
    Map<Domain, CategoryMetrics> domainBuckets = new EnumMap<>(Domain.class);
    for (Domain domain : Domain.values()) {
      domainBuckets.put(domain, new CategoryMetrics());
    }

    for (IssueView issue : issues) {
      double points = IssueAllocationHelper.totalStoryPoints(issue);
      double issueCompletedPoints = IssueAllocationHelper.completedStoryPoints(issue);
      totalStoryPoints += points;
      completedStoryPoints += issueCompletedPoints;

      boolean completed = IssueAllocationHelper.isIssueCompleted(issue);
      if (completed) {
        completedCount++;
      }

      String statusKey = issue.status() + "|" + issue.statusCategory();
      statusBuckets
          .computeIfAbsent(statusKey, key -> new StatusBucket(issue.status(), issue.statusCategory()))
          .add(points);

      for (DomainAllocation allocation : IssueAllocationHelper.effectiveAllocations(issue)) {
        if (allocation.domain() == null || allocation.domain() == Domain.UNKNOWN) {
          continue;
        }
        boolean allocationCompleted = IssueAllocationHelper.isAllocationCompleted(issue, allocation);
        domainBuckets.get(allocation.domain()).add(allocation.storyPoints(), allocationCompleted);
      }
    }

    int total = issues.size();
    int remaining = total - completedCount;
    int totalDomainTouches = domainBuckets.values().stream().mapToInt(metrics -> metrics.count).sum();
    List<IssueView> breakdownIssues = issueTypeBreakdownIssues != null
        ? issueTypeBreakdownIssues
        : issues;

    WorkflowAnalyticsCalculator.Result workflowAnalytics =
        workflowAnalyticsCalculator.calculate(issues, fieldConfig);

    return new AnalyticsResponse(
        jiraSprintId,
        sprintName,
        totalStoryPoints,
        completedStoryPoints,
        totalStoryPoints - completedStoryPoints,
        new IssueCountsDto(total, completedCount, remaining),
        buildIssueTypeBreakdown(breakdownIssues, fieldConfig),
        toStatusDistribution(statusBuckets),
        toDomainBreakdown(domainBuckets, totalDomainTouches, totalStoryPoints),
        workflowAnalytics.stageDistribution(),
        workflowAnalytics.devSubDomainMetrics());
  }

  private BugsVsFeaturesDto buildIssueTypeBreakdown(
      List<IssueView> issues,
      JiraFieldConfig fieldConfig) {
    List<com.sprinklr.sprintplanning.common.model.DevSubDomainIssueTypeProfile> configuredProfiles =
        DevSubDomainAnalysisProfiles.configuredProfiles(fieldConfig);
    List<String> bugTypes = DevSubDomainAnalysisProfiles.bugProfile(configuredProfiles).issueTypes();
    List<String> storyTypes = DevSubDomainAnalysisProfiles.storyProfile(configuredProfiles).issueTypes();

    CategoryMetrics bugs = new CategoryMetrics();
    CategoryMetrics stories = new CategoryMetrics();
    Map<String, CategoryMetrics> otherByType = new LinkedHashMap<>();

    for (IssueView issue : issues) {
      double points = IssueAllocationHelper.totalStoryPoints(issue);
      String issueType = issue.issueType() != null ? issue.issueType() : "Unknown";

      if (DevSubDomainAnalysisProfiles.matchesIssueType(issueType, bugTypes)) {
        bugs.add(points);
      } else if (DevSubDomainAnalysisProfiles.matchesIssueType(issueType, storyTypes)) {
        stories.add(points);
      } else {
        otherByType.computeIfAbsent(issueType, key -> new CategoryMetrics()).add(points);
      }
    }

    List<IssueTypeMetricsDto> otherTypes = otherByType.entrySet().stream()
        .sorted(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER))
        .map(entry -> new IssueTypeMetricsDto(
            entry.getKey(),
            entry.getValue().count,
            round(entry.getValue().storyPoints)))
        .toList();

    return new BugsVsFeaturesDto(bugs.toDto(), stories.toDto(), otherTypes);
  }

  private List<StatusDistributionItemDto> toStatusDistribution(Map<String, StatusBucket> buckets) {
    return buckets.values().stream()
        .sorted(Comparator.comparing(StatusBucket::status).thenComparing(bucket -> bucket.statusCategory().name()))
        .map(bucket -> new StatusDistributionItemDto(
            bucket.status(),
            bucket.statusCategory(),
            bucket.count,
            bucket.storyPoints))
        .toList();
  }

  private List<DomainBreakdownItemDto> toDomainBreakdown(
      Map<Domain, CategoryMetrics> buckets, int totalDomainTouches, double totalStoryPoints) {
    List<DomainBreakdownItemDto> breakdown = new ArrayList<>();
    for (Domain domain : Domain.values()) {
      CategoryMetrics metrics = buckets.get(domain);
      if (metrics.count > 0) {
        breakdown.add(new DomainBreakdownItemDto(
            domain,
            metrics.count,
            round(metrics.storyPoints),
            percentage(metrics.count, totalDomainTouches),
            percentage(metrics.storyPoints, totalStoryPoints),
            metrics.completedCount,
            round(metrics.completedStoryPoints),
            metrics.count - metrics.completedCount,
            round(metrics.storyPoints - metrics.completedStoryPoints),
            percentage(metrics.completedCount, metrics.count),
            percentage(metrics.completedStoryPoints, metrics.storyPoints)));
      }
    }
    return breakdown;
  }

  private double percentage(double numerator, double denominator) {
    if (denominator <= 0) {
      return 0.0;
    }
    return round((numerator / denominator) * 100.0);
  }

  private double storyPointsOrZero(Double storyPoints) {
    return storyPoints != null ? storyPoints : 0.0;
  }

  private double round(double value) {
    return Math.round(value * 100.0) / 100.0;
  }

  private static class CategoryMetrics {
    int count;
    double storyPoints;
    int completedCount;
    double completedStoryPoints;

    void add(double points) {
      add(points, false);
    }

    void add(double points, boolean completed) {
      count++;
      storyPoints += points;
      if (completed) {
        completedCount++;
        completedStoryPoints += points;
      }
    }

    CategoryMetricsDto toDto() {
      return new CategoryMetricsDto(count, storyPoints);
    }
  }

  private static class StatusBucket {
    private final String status;
    private final StatusCategory statusCategory;
    private int count;
    private double storyPoints;

    StatusBucket(String status, StatusCategory statusCategory) {
      this.status = status != null ? status : "Unknown";
      this.statusCategory = statusCategory != null ? statusCategory : StatusCategory.UNKNOWN;
    }

    String status() {
      return status;
    }

    StatusCategory statusCategory() {
      return statusCategory;
    }

    void add(double points) {
      count++;
      storyPoints += points;
    }
  }
}
