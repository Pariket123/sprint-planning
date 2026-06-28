import type { Domain, DomainBreakdownItemDto } from '../../api/types'
import { formatDomain, formatStoryPoints } from '../../utils/format'
import { AnalyticsSummaryCard } from '../common'

const ENGINEERING_DOMAINS: Domain[] = ['BE', 'UI', 'AI']

interface DomainStoryPointsSummaryProps {
  items: DomainBreakdownItemDto[]
}

export function DomainStoryPointsSummary({ items }: DomainStoryPointsSummaryProps) {
  const byDomain = new Map(items.map((item) => [item.domain, item]))
  const engineeringItems = ENGINEERING_DOMAINS.map((domain) => byDomain.get(domain)).filter(
    (item): item is DomainBreakdownItemDto => item !== undefined,
  )

  if (engineeringItems.length === 0) {
    return (
      <p className="text-sm text-gray-500">
        No engineering story points found for the selected scope.
      </p>
    )
  }

  return (
    <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
      {engineeringItems.map((item) => (
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
