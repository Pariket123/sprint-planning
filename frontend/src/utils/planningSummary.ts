import type { PlanningSummaryDto, PlanningViewDto } from '../api/types'

export function buildPlanningSummary(planning: PlanningViewDto): PlanningSummaryDto {
  const metrics = planning.domainMetrics ?? []
  const totalAvailableCapacity = metrics.reduce((sum, metric) => sum + metric.availableCapacity, 0)
  const totalRollover = metrics.reduce((sum, metric) => sum + metric.rollover, 0)

  return {
    jiraSprintId: planning.jiraSprintId,
    totalAvailableCapacity,
    totalRollover,
    totalSelectedStoryPoints: planning.selectedStoryPoints ?? 0,
    totalSelectedIssueCount: planning.selectedIssueCount ?? 0,
    totalCommittedStoryPoints: planning.committedStoryPoints ?? 0,
    totalCommittedIssueCount: planning.committedIssueCount ?? 0,
    totalRoadmapCapacity: planning.totalRoadmapCapacity ?? 0,
    domainMetrics: metrics,
    riskLevel: planning.riskLevel ?? 'LOW',
    capacityAllocationTable: planning.capacityAllocationTable ?? null,
  }
}

export function overCapacityDomains(summary: PlanningSummaryDto): string[] {
  return (summary.domainMetrics ?? [])
    .filter((metric) => metric.capacityRisk === 'OVER_CAPACITY')
    .map((metric) => metric.domain)
}

export function isCommittedOverRoadmapCapacity(summary: PlanningSummaryDto): boolean {
  return summary.totalCommittedStoryPoints > summary.totalRoadmapCapacity
}
