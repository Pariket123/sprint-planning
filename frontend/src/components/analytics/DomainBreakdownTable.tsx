import type { DomainBreakdownItemDto } from '../../api/types'
import { formatDomain, formatPercent, formatStoryPoints } from '../../utils/format'

interface DomainBreakdownTableProps {
  items: DomainBreakdownItemDto[]
}

export function DomainBreakdownTable({ items }: DomainBreakdownTableProps) {
  if (items.length === 0) {
    return <p className="text-sm text-gray-500">No domain breakdown data for this sprint.</p>
  }

  return (
    <div className="overflow-x-auto rounded-lg border border-gray-200">
      <table className="min-w-full divide-y divide-gray-200 text-sm">
        <thead className="bg-gray-50">
          <tr>
            <th className="px-4 py-3 text-left font-medium text-gray-600">Domain</th>
            <th className="px-4 py-3 text-right font-medium text-gray-600">Issues</th>
            <th className="px-4 py-3 text-right font-medium text-gray-600">SP</th>
            <th className="px-4 py-3 text-right font-medium text-gray-600">Issue %</th>
            <th className="px-4 py-3 text-right font-medium text-gray-600">SP %</th>
            <th className="px-4 py-3 text-right font-medium text-gray-600">Done issues</th>
            <th className="px-4 py-3 text-right font-medium text-gray-600">Done SP</th>
            <th className="px-4 py-3 text-right font-medium text-gray-600">Issue completion</th>
            <th className="px-4 py-3 text-right font-medium text-gray-600">SP completion</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-100 bg-white">
          {items.map((item) => (
            <tr key={item.domain}>
              <td className="px-4 py-3 font-medium text-gray-900">{formatDomain(item.domain)}</td>
              <td className="px-4 py-3 text-right text-gray-700">{item.count}</td>
              <td className="px-4 py-3 text-right text-gray-700">
                {formatStoryPoints(item.storyPoints)}
              </td>
              <td className="px-4 py-3 text-right text-gray-700">
                {formatPercent(item.issueCountPercentage)}
              </td>
              <td className="px-4 py-3 text-right text-gray-700">
                {formatPercent(item.storyPointPercentage)}
              </td>
              <td className="px-4 py-3 text-right text-gray-700">{item.completedIssueCount}</td>
              <td className="px-4 py-3 text-right text-gray-700">
                {formatStoryPoints(item.completedStoryPoints)}
              </td>
              <td className="px-4 py-3 text-right text-gray-700">
                {formatPercent(item.issueCompletionPercentage)}
              </td>
              <td className="px-4 py-3 text-right text-gray-700">
                {formatPercent(item.storyPointCompletionPercentage)}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
