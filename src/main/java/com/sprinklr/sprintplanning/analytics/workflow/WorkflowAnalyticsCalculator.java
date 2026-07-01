package com.sprinklr.sprintplanning.analytics.workflow;

import com.sprinklr.sprintplanning.analytics.dto.DevSubDomainItemDto;
import com.sprinklr.sprintplanning.analytics.dto.DevSubDomainMetricsDto;
import com.sprinklr.sprintplanning.analytics.dto.WorkflowStageDistributionDto;
import com.sprinklr.sprintplanning.analytics.dto.WorkflowStageSectionItemDto;
import com.sprinklr.sprintplanning.common.enums.Domain;
import com.sprinklr.sprintplanning.common.model.DevSubDomainConfig;
import com.sprinklr.sprintplanning.common.model.DomainAllocation;
import com.sprinklr.sprintplanning.common.model.IssueView;
import com.sprinklr.sprintplanning.common.model.JiraFieldConfig;
import com.sprinklr.sprintplanning.common.model.WorkflowAnalysisConfig;
import com.sprinklr.sprintplanning.common.model.WorkflowAnalysisSection;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class WorkflowAnalyticsCalculator {

  private static final List<Domain> DEV_SUB_DOMAIN_ORDER =
      List.of(Domain.BE, Domain.UI, Domain.AI);

  private final WorkflowSectionResolver sectionResolver;

  public WorkflowAnalyticsCalculator(WorkflowSectionResolver sectionResolver) {
    this.sectionResolver = sectionResolver;
  }

  public WorkflowStageDistributionDto calculateStageDistribution(
      List<IssueView> issues,
      JiraFieldConfig fieldConfig) {
    WorkflowAnalysisConfig config = workflowConfig(fieldConfig);
    if (config == null || !config.isConfigured()) {
      return null;
    }

    int totalIssues = issues.size();
    Map<String, Integer> counts = initSectionCounts(config);
    int unknownCount = 0;

    for (IssueView issue : issues) {
      Optional<String> sectionKey = sectionResolver.resolveSectionKey(issue.status(), config);
      if (sectionKey.isPresent()) {
        counts.merge(sectionKey.get(), 1, Integer::sum);
      } else {
        unknownCount++;
      }
    }

    List<WorkflowStageSectionItemDto> sections = new ArrayList<>();
    for (WorkflowAnalysisSection section : config.sections()) {
      int count = counts.getOrDefault(section.key(), 0);
      sections.add(new WorkflowStageSectionItemDto(
          section.key(),
          section.label(),
          count,
          totalIssues,
          ratio(count, totalIssues)));
    }

    if (unknownCount > 0) {
      sections.add(new WorkflowStageSectionItemDto(
          "UNKNOWN",
          "Unknown",
          unknownCount,
          totalIssues,
          ratio(unknownCount, totalIssues)));
    }

    return new WorkflowStageDistributionDto(totalIssues, sections);
  }

  public DevSubDomainMetricsDto calculateDevSubDomainMetrics(
      List<IssueView> issues,
      JiraFieldConfig fieldConfig) {
    WorkflowAnalysisConfig config = workflowConfig(fieldConfig);
    if (config == null || config.devSubDomain() == null) {
      return null;
    }

    DevSubDomainConfig subDomainConfig = config.devSubDomain();
    List<IssueView> subDomainPool = issues.stream()
        .filter(issue -> isInSubDomainPool(issue, config))
        .toList();

    List<DevSubDomainItemDto> items = new ArrayList<>();
    for (Domain domain : DEV_SUB_DOMAIN_ORDER) {
      items.add(calculateSubDomainItem(subDomainPool, domain, subDomainConfig, config));
    }

    return new DevSubDomainMetricsDto(subDomainPool.size(), items.stream()
        .filter(item -> item.applicableIssueCount() > 0)
        .toList());
  }

  private DevSubDomainItemDto calculateSubDomainItem(
      List<IssueView> subDomainPool,
      Domain domain,
      DevSubDomainConfig subDomainConfig,
      WorkflowAnalysisConfig workflowConfig) {
    int applicableIssueCount = 0;
    int completedIssueCount = 0;
    double totalStoryPoints = 0;
    double completedStoryPoints = 0;

    for (IssueView issue : subDomainPool) {
      DomainAllocation allocation = findEngineeringAllocation(issue, domain);
      if (allocation == null) {
        continue;
      }
      applicableIssueCount++;
      totalStoryPoints += allocation.storyPoints();
      if (isSubDomainCompleted(issue, allocation, subDomainConfig, workflowConfig)) {
        completedIssueCount++;
        completedStoryPoints += allocation.storyPoints();
      }
    }

    return new DevSubDomainItemDto(
        domain,
        applicableIssueCount,
        completedIssueCount,
        ratio(completedIssueCount, applicableIssueCount),
        round(totalStoryPoints),
        round(completedStoryPoints),
        ratio(completedStoryPoints, totalStoryPoints));
  }

  private boolean isSubDomainCompleted(
      IssueView issue,
      DomainAllocation allocation,
      DevSubDomainConfig subDomainConfig,
      WorkflowAnalysisConfig workflowConfig) {
    if (allocation.completed()) {
      return true;
    }
    return sectionResolver.resolveSectionKey(issue.status(), workflowConfig)
        .map(subDomainConfig::autoCompletesInSection)
        .orElse(false);
  }

  private boolean isInSubDomainPool(IssueView issue, WorkflowAnalysisConfig config) {
    DevSubDomainConfig subDomainConfig = config.devSubDomain();
    if (subDomainConfig == null) {
      return false;
    }
    return sectionResolver.resolveSectionKey(issue.status(), config)
        .map(subDomainConfig::hasSubDomainPoolSection)
        .orElse(false);
  }

  private DomainAllocation findEngineeringAllocation(IssueView issue, Domain domain) {
    List<DomainAllocation> allocations = issue.engineeringAllocations();
    if (allocations == null || allocations.isEmpty()) {
      return null;
    }
    return allocations.stream()
        .filter(allocation -> allocation.domain() == domain)
        .findFirst()
        .orElse(null);
  }

  private Map<String, Integer> initSectionCounts(WorkflowAnalysisConfig config) {
    Map<String, Integer> counts = new LinkedHashMap<>();
    for (WorkflowAnalysisSection section : config.sections()) {
      counts.put(section.key(), 0);
    }
    return counts;
  }

  private WorkflowAnalysisConfig workflowConfig(JiraFieldConfig fieldConfig) {
    return fieldConfig != null ? fieldConfig.workflowAnalysis() : null;
  }

  private double ratio(double numerator, double denominator) {
    if (denominator <= 0) {
      return 0.0;
    }
    return round((numerator / denominator) * 100.0);
  }

  private double round(double value) {
    return Math.round(value * 100.0) / 100.0;
  }
}
