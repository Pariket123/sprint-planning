package com.sprinklr.sprintplanning.analytics.calculator;

import com.sprinklr.sprintplanning.analytics.dto.AnalyticsResponse;
import com.sprinklr.sprintplanning.analytics.dto.BugsVsFeaturesDto;
import com.sprinklr.sprintplanning.analytics.dto.CategoryMetricsDto;
import com.sprinklr.sprintplanning.analytics.dto.DomainBreakdownItemDto;
import com.sprinklr.sprintplanning.analytics.dto.IssueCountsDto;
import com.sprinklr.sprintplanning.analytics.dto.StatusDistributionItemDto;
import com.sprinklr.sprintplanning.common.enums.Domain;
import com.sprinklr.sprintplanning.common.enums.StatusCategory;
import com.sprinklr.sprintplanning.common.model.IssueView;
import com.sprinklr.sprintplanning.common.model.JiraFieldConfig;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class AnalyticsCalculator {

  public AnalyticsResponse calculate(Long jiraSprintId, String sprintName, List<IssueView> issues,
                                     JiraFieldConfig fieldConfig) {
    double totalStoryPoints = 0;
    double completedStoryPoints = 0;
    int completedCount = 0;

    CategoryMetrics bugs = new CategoryMetrics();
    CategoryMetrics features = new CategoryMetrics();
    CategoryMetrics other = new CategoryMetrics();

    Map<String, StatusBucket> statusBuckets = new LinkedHashMap<>();
    Map<Domain, CategoryMetrics> domainBuckets = new EnumMap<>(Domain.class);
    for (Domain domain : Domain.values()) {
      domainBuckets.put(domain, new CategoryMetrics());
    }

    for (IssueView issue : issues) {
      double points = storyPointsOrZero(issue.storyPoints());
      totalStoryPoints += points;

      boolean completed = issue.statusCategory() == StatusCategory.DONE;
      if (completed) {
        completedStoryPoints += points;
        completedCount++;
      }

      switch (classifyIssue(issue, fieldConfig)) {
        case BUG -> bugs.add(points);
        case FEATURE -> features.add(points);
        case OTHER -> other.add(points);
      }

      String statusKey = issue.status() + "|" + issue.statusCategory();
      statusBuckets
          .computeIfAbsent(statusKey, key -> new StatusBucket(issue.status(), issue.statusCategory()))
          .add(points);

      domainBuckets.get(issue.domain()).add(points, completed);
    }

    int total = issues.size();
    int remaining = total - completedCount;

    return new AnalyticsResponse(
        jiraSprintId,
        sprintName,
        totalStoryPoints,
        completedStoryPoints,
        totalStoryPoints - completedStoryPoints,
        new IssueCountsDto(total, completedCount, remaining),
        new BugsVsFeaturesDto(
            bugs.toDto(),
            features.toDto(),
            other.toDto()),
        toStatusDistribution(statusBuckets),
        toDomainBreakdown(domainBuckets, total, totalStoryPoints));
  }

  private IssueCategory classifyIssue(IssueView issue, JiraFieldConfig fieldConfig) {
    String issueType = issue.issueType();
    if (issueType != null && fieldConfig.bugIssueTypes().stream().anyMatch(issueType::equalsIgnoreCase)) {
      return IssueCategory.BUG;
    }
    if (issueType != null && fieldConfig.featureIssueTypes().stream().anyMatch(issueType::equalsIgnoreCase)) {
      return IssueCategory.FEATURE;
    }
    return IssueCategory.OTHER;
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
      Map<Domain, CategoryMetrics> buckets, int totalIssues, double totalStoryPoints) {
    List<DomainBreakdownItemDto> breakdown = new ArrayList<>();
    for (Domain domain : Domain.values()) {
      CategoryMetrics metrics = buckets.get(domain);
      if (metrics.count > 0) {
        breakdown.add(new DomainBreakdownItemDto(
            domain,
            metrics.count,
            round(metrics.storyPoints),
            percentage(metrics.count, totalIssues),
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

  private enum IssueCategory {
    BUG, FEATURE, OTHER
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
