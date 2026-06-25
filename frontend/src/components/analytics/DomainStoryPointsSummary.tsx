import type { DomainBreakdownItemDto } from '../../api/types'
import { formatDomain, formatStoryPoints, sortDomainBreakdown } from '../../utils/format'
import { AnalyticsSummaryCard } from '../common'

interface DomainStoryPointsSummaryProps {
  items: DomainBreakdownItemDto[]
}

export function DomainStoryPointsSummary({ items }: DomainStoryPointsSummaryProps) {
  const sortedItems = sortDomainBreakdown(items)

  if (sortedItems.length === 0) {
    return (
      <p className="text-sm text-gray-500">
        No domain story points found for the selected scope.
      </p>
    )
  }

  return (
    <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
      {sortedItems.map((item) => (
        <AnalyticsSummaryCard
          key={item.domain}
          label={`Total ${formatDomain(item.domain)} story points`}
          value={formatStoryPoints(item.storyPoints)}
          hint={`${formatStoryPoints(item.completedStoryPoints)} done · ${formatStoryPoints(item.remainingStoryPoints)} remaining`}
        />
      ))}
    </div>
  )
}
