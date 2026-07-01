import type { PlanningSummaryDto, PlanningViewDto, RiskLevel } from '../api/types'

const HIGH_UTILIZATION_THRESHOLD = 0.9
const MEDIUM_UTILIZATION_THRESHOLD = 0.75

function determineRiskLevel(totalSelected: number, totalAvailable: number): RiskLevel {
  if (totalAvailable <= 0) {
    return totalSelected > 0 ? 'HIGH' : 'LOW'
  }

  const utilization = totalSelected / totalAvailable
  if (utilization > HIGH_UTILIZATION_THRESHOLD) {
    return 'HIGH'
  }
  if (utilization > MEDIUM_UTILIZATION_THRESHOLD) {
    return 'MEDIUM'
  }
  return 'LOW'
}

export function buildPlanningSummary(planning: PlanningViewDto): PlanningSummaryDto {
  const metrics = planning.domainMetrics ?? []
  const totalAvailableCapacity = metrics.reduce((sum, metric) => sum + metric.availableCapacity, 0)
  const totalRollover = metrics.reduce((sum, metric) => sum + metric.rollover, 0)
  const totalSelectedStoryPoints = metrics.reduce(
    (sum, metric) => sum + metric.selectedStoryPoints,
    0,
  )
  const totalSelectedIssueCount = metrics.reduce(
    (sum, metric) => sum + metric.selectedIssueCount,
    0,
  )

  return {
    jiraSprintId: planning.jiraSprintId,
    totalAvailableCapacity,
    totalRollover,
    totalSelectedStoryPoints,
    totalSelectedIssueCount,
    domainMetrics: metrics,
    riskLevel: determineRiskLevel(totalSelectedStoryPoints, totalAvailableCapacity),
    capacityAllocationTable: planning.capacityAllocationTable ?? null,
  }
}
