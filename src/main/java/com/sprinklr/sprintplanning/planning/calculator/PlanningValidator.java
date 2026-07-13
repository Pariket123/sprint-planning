package com.sprinklr.sprintplanning.planning.calculator;

import com.sprinklr.sprintplanning.planning.config.PlanningProperties;
import com.sprinklr.sprintplanning.planning.dto.CapacityRiskStatus;
import com.sprinklr.sprintplanning.planning.dto.DomainPlanningMetricsDto;
import com.sprinklr.sprintplanning.planning.dto.PlanningSummaryDto;
import com.sprinklr.sprintplanning.planning.dto.PlanningValidationResultDto;
import com.sprinklr.sprintplanning.planning.dto.PlanningWarningCode;
import com.sprinklr.sprintplanning.planning.dto.PlanningWarningDto;
import com.sprinklr.sprintplanning.planning.dto.RiskLevel;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PlanningValidator {

  private final PlanningProperties properties;

  public PlanningValidator(PlanningProperties properties) {
    this.properties = properties;
  }

  public PlanningValidationResultDto validate(PlanningSummaryDto summary) {
    List<PlanningWarningDto> warnings = new ArrayList<>();

    for (DomainPlanningMetricsDto metrics : summary.domainMetrics()) {
      if (metrics.selectedStoryPoints() > metrics.availableCapacity()) {
        warnings.add(new PlanningWarningDto(
            PlanningWarningCode.OVER_CAPACITY,
            "Selected story points exceed available capacity for " + metrics.domain(),
            metrics.domain()));
      }
      if (metrics.capacityRisk() == CapacityRiskStatus.OVER_CAPACITY) {
        warnings.add(new PlanningWarningDto(
            PlanningWarningCode.OVER_CAPACITY,
            "Committed story points exceed available capacity for " + metrics.domain(),
            metrics.domain()));
      }
    }

    if (summary.totalSelectedStoryPoints() > 0) {
      for (DomainPlanningMetricsDto metrics : summary.domainMetrics()) {
        double share = metrics.selectedStoryPoints() / summary.totalSelectedStoryPoints();
        if (share > properties.getDomainImbalanceThreshold()) {
          warnings.add(new PlanningWarningDto(
              PlanningWarningCode.DOMAIN_IMBALANCE,
              metrics.domain() + " accounts for more than "
                  + (int) (properties.getDomainImbalanceThreshold() * 100)
                  + "% of selected story points",
              metrics.domain()));
        }
      }
    }

    if (summary.totalAvailableCapacity() > 0) {
      double utilization = summary.totalSelectedStoryPoints() / summary.totalAvailableCapacity();
      if (utilization > properties.getHighUtilizationThreshold()) {
        warnings.add(new PlanningWarningDto(
            PlanningWarningCode.HIGH_UTILIZATION,
            "Total selected story points exceed "
                + (int) (properties.getHighUtilizationThreshold() * 100)
                + "% of available capacity",
            null));
      }
    }

    RiskLevel riskLevel = summary.riskLevel();
    if (warnings.stream().anyMatch(w -> w.code() == PlanningWarningCode.OVER_CAPACITY)) {
      riskLevel = RiskLevel.HIGH;
    }

    return new PlanningValidationResultDto(warnings, riskLevel);
  }
}
