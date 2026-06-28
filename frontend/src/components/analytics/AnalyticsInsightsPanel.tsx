import type { AnalyticsResponse } from '../../api/types'
import { AnalyticsSummaryCard } from '../common'
import { BugsVsFeaturesSection } from './BugsVsFeaturesSection'
import { DevSubDomainMetricsPanel } from './DevSubDomainMetricsPanel'
import { DomainStoryPointsSummary } from './DomainStoryPointsSummary'
import { StatusDistributionTable } from './StatusDistributionTable'
import { WorkflowStageDistributionPanel } from './WorkflowStageDistributionPanel'

interface AnalyticsInsightsPanelProps {
  analytics: AnalyticsResponse
}

export function AnalyticsInsightsPanel({ analytics }: AnalyticsInsightsPanelProps) {
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

      {analytics.workflowStageDistribution && (
        <section className="rounded-xl border border-gray-200 bg-white p-5 shadow-sm">
          <h2 className="text-sm font-semibold text-gray-900">Workflow stage distribution</h2>
          <p className="mt-1 text-sm text-gray-600">
            Current section per issue based on configured workflow statuses.
          </p>
          <div className="mt-4">
            <WorkflowStageDistributionPanel distribution={analytics.workflowStageDistribution} />
          </div>
        </section>
      )}

      {analytics.devSubDomainMetrics && (
        <section className="rounded-xl border border-gray-200 bg-white p-5 shadow-sm">
          <h2 className="text-sm font-semibold text-gray-900">Dev sub-domain completion</h2>
          <p className="mt-1 text-sm text-gray-600">
            BE, UI, and AI completion among issues in dev-or-beyond (checkbox or auto-complete in QA+).
          </p>
          <div className="mt-4">
            <DevSubDomainMetricsPanel metrics={analytics.devSubDomainMetrics} />
          </div>
        </section>
      )}

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
    </div>
  )
}
