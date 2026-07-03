import type { IssueView } from '../../api/types'
import { formatIssueDomain, formatStoryPoints } from '../../utils/format'
import { StatusCategoryBadge } from '../common/StatusCategoryBadge'

interface PlanningIssueTableProps {
  issues: IssueView[]
  title?: string
  selectable?: boolean
  selectedKeys?: string[]
  onSelectionChange?: (keys: string[]) => void
}

export function PlanningIssueTable({
  issues,
  title,
  selectable = false,
  selectedKeys = [],
  onSelectionChange,
}: PlanningIssueTableProps) {
  if (issues.length === 0) {
    return <p className="text-sm text-gray-500">No issues to display.</p>
  }

  const allSelected = issues.length > 0 && issues.every((issue) => selectedKeys.includes(issue.key))

  const toggleAll = () => {
    if (!onSelectionChange) {
      return
    }
    onSelectionChange(allSelected ? [] : issues.map((issue) => issue.key))
  }

  const toggleOne = (key: string) => {
    if (!onSelectionChange) {
      return
    }
    if (selectedKeys.includes(key)) {
      onSelectionChange(selectedKeys.filter((item) => item !== key))
    } else {
      onSelectionChange([...selectedKeys, key])
    }
  }

  return (
    <div>
      {title && <h3 className="mb-3 text-sm font-semibold text-gray-900">{title}</h3>}
      <div className="overflow-x-auto rounded-lg border border-gray-200">
        <table className="min-w-full divide-y divide-gray-200 text-sm">
          <thead className="bg-gray-50">
            <tr>
              {selectable && (
                <th className="px-4 py-3 text-left">
                  <input
                    type="checkbox"
                    checked={allSelected}
                    onChange={toggleAll}
                    aria-label="Select all issues"
                  />
                </th>
              )}
              <th className="px-4 py-3 text-left font-medium text-gray-600">Key</th>
              <th className="px-4 py-3 text-left font-medium text-gray-600">Summary</th>
              <th className="px-4 py-3 text-left font-medium text-gray-600">Type</th>
              <th className="px-4 py-3 text-left font-medium text-gray-600">Status</th>
              <th className="px-4 py-3 text-left font-medium text-gray-600">Category</th>
              <th className="px-4 py-3 text-right font-medium text-gray-600">SP</th>
              <th className="px-4 py-3 text-left font-medium text-gray-600">Domain</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100 bg-white">
            {issues.map((issue) => (
              <tr key={issue.key}>
                {selectable && (
                  <td className="px-4 py-3">
                    <input
                      type="checkbox"
                      checked={selectedKeys.includes(issue.key)}
                      onChange={() => toggleOne(issue.key)}
                      aria-label={`Select ${issue.key}`}
                    />
                  </td>
                )}
                <td className="px-4 py-3 font-medium text-brand-600">{issue.key}</td>
                <td className="max-w-xs truncate px-4 py-3 text-gray-900">{issue.summary}</td>
                <td className="px-4 py-3 text-gray-700">{issue.issueType}</td>
                <td className="px-4 py-3 text-gray-700">{issue.status}</td>
                <td className="px-4 py-3">
                  <StatusCategoryBadge category={issue.statusCategory} />
                </td>
                <td className="px-4 py-3 text-right text-gray-700">
                  {formatStoryPoints(issue.storyPoints)}
                </td>
                <td className="px-4 py-3 text-gray-700">
                  {formatIssueDomain(issue.domain, issue.domainLabel)}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
