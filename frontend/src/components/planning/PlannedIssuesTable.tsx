import type { PlannedIssueViewDto } from '../../api/types'
import { formatDomain, formatStoryPoints } from '../../utils/format'
import { StatusCategoryBadge } from '../common/StatusCategoryBadge'

interface PlannedIssuesTableProps {
  issues: PlannedIssueViewDto[]
}

function rolloverDisplay(issue: PlannedIssueViewDto): { label: string; style: string } {
  const inPlannedSprint = issue.currentSprintId === issue.plannedSprintId

  if (inPlannedSprint) {
    return {
      label: 'On plan',
      style: 'bg-emerald-50 text-emerald-700 ring-emerald-200',
    }
  }
  if (issue.rolledOver) {
    return {
      label: 'Rolled over',
      style: 'bg-rollover-50 text-rollover-700 ring-rollover-200',
    }
  }
  return {
    label: 'Not in sprint',
    style: 'bg-brand-50 text-brand-600 ring-brand-200',
  }
}

export function PlannedIssuesTable({ issues }: PlannedIssuesTableProps) {
  if (issues.length === 0) {
    return <p className="text-sm text-gray-500">No planned issues yet.</p>
  }

  return (
    <div className="overflow-x-auto rounded-lg border border-gray-200">
      <table className="min-w-full divide-y divide-gray-200 text-sm">
        <thead className="bg-gray-50">
          <tr>
            <th className="px-4 py-3 text-left font-medium text-gray-600">Key</th>
            <th className="px-4 py-3 text-left font-medium text-gray-600">Summary</th>
            <th className="px-4 py-3 text-left font-medium text-gray-600">Status</th>
            <th className="px-4 py-3 text-right font-medium text-gray-600">SP</th>
            <th className="px-4 py-3 text-left font-medium text-gray-600">Domain</th>
            <th className="px-4 py-3 text-left font-medium text-gray-600">Current sprint</th>
            <th className="px-4 py-3 text-left font-medium text-gray-600">Rollover</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-100 bg-white">
          {issues.map((issue) => {
            const rollover = rolloverDisplay(issue)

            return (
            <tr key={issue.key}>
              <td className="px-4 py-3 font-medium text-brand-600">{issue.key}</td>
              <td className="max-w-xs truncate px-4 py-3 text-gray-900">{issue.summary}</td>
              <td className="px-4 py-3">
                <div className="flex flex-col gap-1">
                  <span className="text-gray-700">{issue.status}</span>
                  <StatusCategoryBadge category={issue.statusCategory} />
                </div>
              </td>
              <td className="px-4 py-3 text-right text-gray-700">
                {formatStoryPoints(issue.storyPoints)}
              </td>
              <td className="px-4 py-3 text-gray-700">{formatDomain(issue.domain)}</td>
              <td className="px-4 py-3 text-gray-700">
                {issue.currentSprintId ?? 'Backlog'}
              </td>
              <td className="px-4 py-3">
                <span
                  className={`inline-flex rounded-full px-2 py-0.5 text-xs font-medium ring-1 ${rollover.style}`}
                >
                  {rollover.label}
                </span>
              </td>
            </tr>
            )
          })}
        </tbody>
      </table>
    </div>
  )
}
