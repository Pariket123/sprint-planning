import type { PlanningSummaryDto } from '../../api/types'
import { formatDomain, formatStoryPoints } from '../../utils/format'
import { isCommittedOverRoadmapCapacity, overCapacityDomains } from '../../utils/planningSummary'

interface PlanningCapacityGuidanceProps {
  summary: PlanningSummaryDto
}

export function PlanningCapacityGuidance({ summary }: PlanningCapacityGuidanceProps) {
  const overCapacity = isCommittedOverRoadmapCapacity(summary)
  const hotDomains = overCapacityDomains(summary)
  const committedGap = summary.totalCommittedStoryPoints - summary.totalRoadmapCapacity
  const scopeDrift =
    summary.totalSelectedStoryPoints !== summary.totalCommittedStoryPoints ||
    summary.totalSelectedIssueCount !== summary.totalCommittedIssueCount

  if (!overCapacity && !scopeDrift) {
    return null
  }

  return (
    <div className="space-y-3">
      {overCapacity && (
        <div
          className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-900"
          role="alert"
        >
          <p className="font-medium">Committed work exceeds roadmap capacity</p>
          <p className="mt-1">
            Committed {formatStoryPoints(summary.totalCommittedStoryPoints)} SP vs roadmap cap{' '}
            {formatStoryPoints(summary.totalRoadmapCapacity)} SP
            {committedGap > 0 && (
              <>
                {' '}
                (reduce by about {formatStoryPoints(committedGap)} SP)
              </>
            )}
            .
          </p>
          {hotDomains.length > 0 && (
            <p className="mt-1">
              Over-capacity domains: {hotDomains.map((domain) => formatDomain(domain)).join(', ')}.
              Use <span className="font-medium">Uncommit from plan</span> on the Issues tab to lower
              utilization without removing issues from Jira, or move them to backlog.
            </p>
          )}
        </div>
      )}

      {scopeDrift && (
        <div className="rounded-lg border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-900">
          <p className="font-medium">Selected scope and commitment differ</p>
          <p className="mt-1">
            Selected {formatStoryPoints(summary.totalSelectedStoryPoints)} SP (
            {summary.totalSelectedIssueCount} issues) vs committed{' '}
            {formatStoryPoints(summary.totalCommittedStoryPoints)} SP (
            {summary.totalCommittedIssueCount} issues). Utilization follows committed work — use{' '}
            <span className="font-medium">Uncommit from plan</span> on the Issues tab to reduce it.
            EXCLUDE only affects selected scope.
          </p>
        </div>
      )}
    </div>
  )
}
