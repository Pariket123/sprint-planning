import type { DevSubDomainMetricsDto } from '../../api/types'
import { formatDomain, formatStoryPoints } from '../../utils/format'

interface DevSubDomainMetricsPanelProps {
  metrics: DevSubDomainMetricsDto
}

export function DevSubDomainMetricsPanel({ metrics }: DevSubDomainMetricsPanelProps) {
  if (metrics.subDomains.length === 0) {
    return <p className="text-sm text-gray-500">No engineering sub-domain data in the dev-or-beyond pool.</p>
  }

  return (
    <div className="space-y-3">
      <p className="text-sm text-gray-600">
        Dev-or-beyond pool: {metrics.subDomainPoolIssueCount} issue
        {metrics.subDomainPoolIssueCount === 1 ? '' : 's'}
      </p>
      <div className="overflow-x-auto">
        <table className="min-w-full divide-y divide-gray-200 text-sm">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-4 py-3 text-left font-medium text-gray-600">Sub-domain</th>
              <th className="px-4 py-3 text-right font-medium text-gray-600">Issues</th>
              <th className="px-4 py-3 text-right font-medium text-gray-600">Story points</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100 bg-white">
            {metrics.subDomains.map((item) => (
              <tr key={item.domain}>
                <td className="px-4 py-3 font-medium text-gray-900">{formatDomain(item.domain)}</td>
                <td className="px-4 py-3 text-right text-gray-700">
                  {item.completedIssueCount}/{item.applicableIssueCount} ({item.issueCompletionRatio.toFixed(1)}%)
                </td>
                <td className="px-4 py-3 text-right text-gray-700">
                  {formatStoryPoints(item.completedStoryPoints)}/{formatStoryPoints(item.totalStoryPoints)} (
                  {item.storyPointCompletionRatio.toFixed(1)}%)
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
