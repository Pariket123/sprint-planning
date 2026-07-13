package com.sprinklr.sprintplanning.planning.calculator;

import com.sprinklr.sprintplanning.common.model.IssueView;
import com.sprinklr.sprintplanning.common.util.IssueAllocationHelper;
import com.sprinklr.sprintplanning.planning.config.PlanningProperties;
import com.sprinklr.sprintplanning.planning.dto.CapacityRiskStatus;
import com.sprinklr.sprintplanning.planning.dto.RiskLevel;

import java.util.List;

final class PlanningMathSupport {

  private final PlanningProperties properties;

  PlanningMathSupport(PlanningProperties properties) {
    this.properties = properties;
  }

  double round(double value) {
    return Math.round(value * 100.0) / 100.0;
  }

  double calculateUtilizationPercent(double committedStoryPoints, double availableCapacity) {
    if (availableCapacity <= 0) {
      return committedStoryPoints > 0 ? 100.0 : 0.0;
    }
    return (committedStoryPoints / availableCapacity) * 100.0;
  }

  CapacityRiskStatus determineCapacityRisk(double committedStoryPoints, double availableCapacity) {
    if (availableCapacity <= 0) {
      return committedStoryPoints > 0 ? CapacityRiskStatus.OVER_CAPACITY : CapacityRiskStatus.OK;
    }
    double utilization = committedStoryPoints / availableCapacity;
    if (utilization > 1.0) {
      return CapacityRiskStatus.OVER_CAPACITY;
    }
    if (utilization >= properties.getHighUtilizationThreshold()) {
      return CapacityRiskStatus.NEAR_CAPACITY;
    }
    return CapacityRiskStatus.OK;
  }

  RiskLevel determineRiskLevel(double totalSelected, double totalAvailable) {
    if (totalAvailable <= 0) {
      return totalSelected > 0 ? RiskLevel.HIGH : RiskLevel.LOW;
    }
    double utilization = totalSelected / totalAvailable;
    if (utilization > properties.getHighUtilizationThreshold()) {
      return RiskLevel.HIGH;
    }
    if (utilization > properties.getMediumUtilizationThreshold()) {
      return RiskLevel.MEDIUM;
    }
    return RiskLevel.LOW;
  }

  double sumUniqueIssueStoryPoints(List<IssueView> issues, PlanningDomainSupport domainSupport) {
    return domainSupport.nullSafeIssues(issues).stream()
        .mapToDouble(IssueAllocationHelper::totalStoryPoints)
        .sum();
  }

  int countUniqueIssues(List<IssueView> issues, PlanningDomainSupport domainSupport) {
    return (int) domainSupport.nullSafeIssues(issues).stream()
        .map(IssueView::key)
        .filter(key -> key != null && !key.isBlank())
        .distinct()
        .count();
  }
}
