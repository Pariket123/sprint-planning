import type { AnalyticsResponse } from '../../api/types'
import { EmptyState, AnalyticsSummaryCard } from '../common'
import { BugsVsFeaturesSection } from './BugsVsFeaturesSection'
import { DomainBreakdownTable } from './DomainBreakdownTable'
import { DomainMetricCard } from './DomainMetricCard'
import { DomainStoryPointsSummary } from './DomainStoryPointsSummary'
import { StatusDistributionTable } from './StatusDistributionTable'
import { sortDomainBreakdown } from '../../utils/format'

interface AnalyticsInsightsPanelProps {
  analytics: AnalyticsResponse
  domainBreakdownDescription: string
}

export function AnalyticsInsightsPanel({
  analytics,
  domainBreakdownDescription,
}: AnalyticsInsightsPanelProps) {
  const domainItems = sortDomainBreakdown(analytics.domainBreakdown)

  return (
    <div className="space-y-6">
      <section>
        <h2 className="mb-4 text-sm font-semibold text-gray-900">Story points by domain</h2>
        <DomainStoryPointsSummary items={analytics.domainBreakdown} />
      </section>

      <section>
        <h2 className="mb-4 text-sm font-semibold text-gray-900">Issues</h2>
        <div className="grid gap-4 sm:grid-cols-3">
          <AnalyticsSummaryCard label="Total issues" value={analytics.issueCounts.total} />
          <AnalyticsSummaryCard label="Completed issues" value={analytics.issueCounts.completed} />
          <AnalyticsSummaryCard label="Remaining issues" value={analytics.issueCounts.remaining} />
        </div>
      </section>

      <section className="rounded-xl border border-gray-200 bg-white p-5 shadow-sm">
        <h2 className="text-sm font-semibold text-gray-900">Status distribution</h2>
        <div className="mt-4">
          <StatusDistributionTable items={analytics.statusDistribution} />
        </div>
      </section>

      <section className="rounded-xl border border-gray-200 bg-white p-5 shadow-sm">
        <h2 className="text-sm font-semibold text-gray-900">Bugs vs features</h2>
        <div className="mt-4">
          <BugsVsFeaturesSection data={analytics.bugsVsFeatures} />
        </div>
      </section>

      <section>
        <div className="mb-4">
          <h2 className="text-sm font-semibold text-gray-900">Domain breakdown</h2>
          <p className="mt-1 text-sm text-gray-600">{domainBreakdownDescription}</p>
        </div>

        {domainItems.length === 0 ? (
          <EmptyState
            title="No domain data"
            description="Issues in this scope do not have domain mapping."
          />
        ) : (
          <>
            <div className="grid gap-4 lg:grid-cols-2">
              {domainItems.map((item) => (
                <DomainMetricCard key={item.domain} item={item} />
              ))}
            </div>

            <div className="mt-4">
              <DomainBreakdownTable items={domainItems} />
            </div>
          </>
        )}
      </section>
    </div>
  )
}
