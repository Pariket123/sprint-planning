import type { DomainPlanningMetricsDto } from '../../api/types'
import { formatDomain, formatPercent, formatStoryPoints } from '../../utils/format'
import { RiskBadge } from './RiskBadge'

interface DomainMetricsTableProps {
  metrics: DomainPlanningMetricsDto[] | null | undefined
}

export function DomainMetricsTable({ metrics }: DomainMetricsTableProps) {
  const rows = metrics ?? []

  if (rows.length === 0) {
    return <p className="text-sm text-gray-500">No domain metrics available.</p>
  }

  return (
    <div className="overflow-x-auto rounded-lg border border-gray-200">
      <table className="min-w-full divide-y divide-gray-200 text-sm">
        <thead className="bg-gray-50">
          <tr>
            <th className="px-4 py-3 text-left font-medium text-gray-600">Domain</th>
            <th className="px-4 py-3 text-right font-medium text-gray-600">Capacity</th>
            <th className="px-4 py-3 text-right font-medium text-gray-600">Rollover</th>
            <th className="px-4 py-3 text-right font-medium text-gray-600">Selected SP</th>
            <th className="px-4 py-3 text-right font-medium text-gray-600">Issues</th>
            <th className="px-4 py-3 text-right font-medium text-gray-600">Suggested</th>
            <th className="px-4 py-3 text-right font-medium text-gray-600">Committed SP</th>
            <th className="px-4 py-3 text-right font-medium text-gray-600">Utilization</th>
            <th className="px-4 py-3 text-left font-medium text-gray-600">Risk</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-100 bg-white">
          {rows.map((metric) => (
            <tr key={metric.domain}>
              <td className="px-4 py-3 font-medium text-gray-900">
                {formatDomain(metric.domain)}
              </td>
              <td className="px-4 py-3 text-right text-gray-700">
                {formatStoryPoints(metric.availableCapacity)}
              </td>
              <td className="px-4 py-3 text-right text-gray-700">
                {formatStoryPoints(metric.rollover)}
              </td>
              <td className="px-4 py-3 text-right text-gray-700">
                {formatStoryPoints(metric.selectedStoryPoints)}
              </td>
              <td className="px-4 py-3 text-right text-gray-700">{metric.selectedIssueCount}</td>
              <td className="px-4 py-3 text-right text-gray-700">
                {formatStoryPoints(metric.suggestedCommitment)}
              </td>
              <td className="px-4 py-3 text-right text-gray-700">
                {formatStoryPoints(metric.committedStoryPoints)}
              </td>
              <td className="px-4 py-3 text-right text-gray-700">
                {formatPercent(metric.utilizationPercent)}
              </td>
              <td className="px-4 py-3">
                <RiskBadge risk={metric.capacityRisk} />
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
