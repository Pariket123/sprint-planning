import type { DomainBreakdownItemDto } from '../../api/types'
import { formatDomain, formatPercent, formatStoryPoints } from '../../utils/format'
import { PercentageBar } from '../common/PercentageBar'

interface DomainMetricCardProps {
  item: DomainBreakdownItemDto
}

export function DomainMetricCard({ item }: DomainMetricCardProps) {
  return (
    <div className="rounded-xl border border-gray-200 bg-white p-5 shadow-sm">
      <div className="flex items-start justify-between gap-3">
        <div>
          <h3 className="text-sm font-semibold text-gray-900">{formatDomain(item.domain)}</h3>
          <p className="mt-1 text-xs text-gray-500">
            {item.count} issues · {formatStoryPoints(item.storyPoints)} SP
          </p>
        </div>
        <div className="text-right text-xs text-gray-500">
          <p>{formatPercent(item.issueCountPercentage)} of issues</p>
          <p>{formatPercent(item.storyPointPercentage)} of SP</p>
        </div>
      </div>

      <div className="mt-4 space-y-3">
        <PercentageBar
          label="Issue completion"
          value={item.issueCompletionPercentage}
          displayValue={`${item.completedIssueCount}/${item.count} (${formatPercent(item.issueCompletionPercentage)})`}
          colorClass="bg-emerald-500"
        />
        <PercentageBar
          label="Story point completion"
          value={item.storyPointCompletionPercentage}
          displayValue={`${formatStoryPoints(item.completedStoryPoints)}/${formatStoryPoints(item.storyPoints)} SP (${formatPercent(item.storyPointCompletionPercentage)})`}
          colorClass="bg-brand-600"
        />
      </div>

      <dl className="mt-4 grid grid-cols-2 gap-3 border-t border-gray-100 pt-4 text-xs">
        <Metric label="Completed issues" value={String(item.completedIssueCount)} />
        <Metric label="Remaining issues" value={String(item.remainingIssueCount)} />
        <Metric label="Completed SP" value={formatStoryPoints(item.completedStoryPoints)} />
        <Metric label="Remaining SP" value={formatStoryPoints(item.remainingStoryPoints)} />
      </dl>
    </div>
  )
}

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <dt className="text-gray-500">{label}</dt>
      <dd className="mt-0.5 font-medium text-gray-900">{value}</dd>
    </div>
  )
}
