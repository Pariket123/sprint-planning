import type { Domain, DomainBreakdownItemDto } from '../../api/types'
import { formatDomain, formatStoryPoints } from '../../utils/format'
import { AnalyticsSummaryCard } from '../common'

const ENGINEERING_DOMAINS: Domain[] = ['BE', 'UI', 'AI']
const DOMAIN_SUMMARY_ORDER: Domain[] = ['BE', 'UI', 'AI', 'QA']

interface DomainStoryPointsSummaryProps {
  items: DomainBreakdownItemDto[]
  includeQa?: boolean
}

export function DomainStoryPointsSummary({
  items,
  includeQa = false,
}: DomainStoryPointsSummaryProps) {
  const byDomain = new Map(items.map((item) => [item.domain, item]))
  const domains = includeQa ? DOMAIN_SUMMARY_ORDER : ENGINEERING_DOMAINS
  const summaryItems = domains.map((domain) => byDomain.get(domain)).filter(
    (item): item is DomainBreakdownItemDto => item !== undefined,
  )

  if (summaryItems.length === 0) {
    return (
      <p className="text-sm text-gray-500">
        No engineering story points found for the selected scope.
      </p>
    )
  }

  return (
    <div className={`grid gap-4 sm:grid-cols-2 ${includeQa ? 'xl:grid-cols-4' : 'xl:grid-cols-3'}`}>
      {summaryItems.map((item) => (
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
