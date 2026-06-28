import { useCallback, useEffect, useState } from 'react'
import { ArrowLeft } from 'lucide-react'
import {
  ApiError,
  getRelease,
  getReleaseCapacityMetrics,
  getReleaseIssuesAnalytics,
  searchIssuesInRelease,
  updateReleaseCapacity,
} from '../../api'
import type {
  AnalyticsResponse,
  IssueSearchPageDto,
  ReleaseCapacitySummaryDto,
  ReleaseResponse,
} from '../../api/types'
import { AnalyticsInsightsPanel } from '../analytics/AnalyticsInsightsPanel'
import { PageErrorState, PageLoadingState } from '../common'
import { ReleaseCapacityEditor } from './ReleaseCapacityEditor'
import { DomainMetricsTable } from '../planning/DomainMetricsTable'
import { IssueTable } from '../issues/IssueTable'
import { formatStoryPoints } from '../../utils/format'

type ReleaseViewTab = 'issues' | 'analysis' | 'capacity'

interface ReleaseIssuesTabProps {
  podId: string
  release: ReleaseResponse
  onBack: () => void
}

export function ReleaseIssuesTab({ podId, release, onBack }: ReleaseIssuesTabProps) {
  const [releaseConfig, setReleaseConfig] = useState(release)
  const [activeTab, setActiveTab] = useState<ReleaseViewTab>('issues')
  const [draftAdditionalJql, setDraftAdditionalJql] = useState('')
  const [appliedAdditionalJql, setAppliedAdditionalJql] = useState('')
  const [page, setPage] = useState<IssueSearchPageDto | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [startAt, setStartAt] = useState(0)
  const [analytics, setAnalytics] = useState<AnalyticsResponse | null>(null)
  const [analyticsLoading, setAnalyticsLoading] = useState(false)
  const [analyticsError, setAnalyticsError] = useState<string | null>(null)
  const [capacityMetrics, setCapacityMetrics] = useState<ReleaseCapacitySummaryDto | null>(null)
  const [capacityLoading, setCapacityLoading] = useState(false)
  const [capacityError, setCapacityError] = useState<string | null>(null)
  const pageSize = 50

  useEffect(() => {
    setReleaseConfig(release)
  }, [release])

  const buildReleaseRequest = useCallback(
    (jql: string) => ({ additionalJql: jql.trim() || null }),
    [],
  )

  const runSearch = useCallback(
    async (offset = 0, jql = appliedAdditionalJql, updateAppliedFilters = false) => {
      setLoading(true)
      setError(null)

      if (updateAppliedFilters) {
        setAppliedAdditionalJql(jql)
      }

      try {
        const result = await searchIssuesInRelease(
          podId,
          releaseConfig.id,
          buildReleaseRequest(jql),
          { startAt: offset, maxResults: pageSize },
        )
        setPage(result)
        setStartAt(offset)
      } catch (err) {
        setPage(null)
        setError(err instanceof ApiError ? err.message : 'Failed to search release issues.')
      } finally {
        setLoading(false)
      }
    },
    [podId, releaseConfig.id, appliedAdditionalJql, buildReleaseRequest, pageSize],
  )

  const loadAnalytics = useCallback(
    async (jql = appliedAdditionalJql) => {
      setAnalyticsLoading(true)
      setAnalyticsError(null)

      try {
        const result = await getReleaseIssuesAnalytics(
          podId,
          releaseConfig.id,
          buildReleaseRequest(jql),
        )
        setAnalytics(result)
      } catch (err) {
        setAnalytics(null)
        setAnalyticsError(
          err instanceof ApiError ? err.message : 'Failed to load release analytics.',
        )
      } finally {
        setAnalyticsLoading(false)
      }
    },
    [podId, releaseConfig.id, appliedAdditionalJql, buildReleaseRequest],
  )

  const loadCapacityMetrics = useCallback(
    async (jql = appliedAdditionalJql) => {
      setCapacityLoading(true)
      setCapacityError(null)

      try {
        const result = await getReleaseCapacityMetrics(
          podId,
          releaseConfig.id,
          buildReleaseRequest(jql),
        )
        setCapacityMetrics(result)
      } catch (err) {
        setCapacityMetrics(null)
        setCapacityError(
          err instanceof ApiError ? err.message : 'Failed to load release capacity metrics.',
        )
      } finally {
        setCapacityLoading(false)
      }
    },
    [podId, releaseConfig.id, appliedAdditionalJql, buildReleaseRequest],
  )

  useEffect(() => {
    setActiveTab('issues')
    setDraftAdditionalJql('')
    setAppliedAdditionalJql('')
    setPage(null)
    setAnalytics(null)
    setAnalyticsError(null)
    setCapacityMetrics(null)
    setCapacityError(null)

    void (async () => {
      setLoading(true)
      setError(null)
      try {
        const result = await searchIssuesInRelease(
          podId,
          release.id,
          { additionalJql: null },
          { startAt: 0, maxResults: pageSize },
        )
        setPage(result)
        setStartAt(0)
      } catch (err) {
        setPage(null)
        setError(err instanceof ApiError ? err.message : 'Failed to search release issues.')
      } finally {
        setLoading(false)
      }
    })()
  }, [release.id, podId, pageSize])

  useEffect(() => {
    if (activeTab !== 'analysis') {
      return
    }
    void loadAnalytics(appliedAdditionalJql)
  }, [activeTab, appliedAdditionalJql, loadAnalytics])

  useEffect(() => {
    if (activeTab !== 'capacity') {
      return
    }
    void loadCapacityMetrics(appliedAdditionalJql)
  }, [activeTab, appliedAdditionalJql, loadCapacityMetrics])

  const handleSearch = () => {
    void runSearch(0, draftAdditionalJql, true)
  }

  const handleCapacitySaved = async () => {
    const refreshed = await getRelease(podId, releaseConfig.id)
    setReleaseConfig(refreshed)
    await loadCapacityMetrics(appliedAdditionalJql)
  }

  return (
    <div className="space-y-6">
      <button
        type="button"
        onClick={onBack}
        className="inline-flex items-center gap-2 text-sm font-medium text-brand-600 hover:text-brand-700 hover:underline"
      >
        <ArrowLeft className="h-4 w-4" aria-hidden="true" />
        Back to releases
      </button>

      <section className="rounded-xl border border-gray-200 bg-white p-5 shadow-sm">
        <h2 className="text-lg font-semibold text-gray-900">{releaseConfig.name}</h2>
        {releaseConfig.description && (
          <p className="mt-1 text-sm text-gray-600">{releaseConfig.description}</p>
        )}
        <div className="mt-4 flex flex-wrap gap-4 text-sm text-gray-600">
          {releaseConfig.durationDays != null && (
            <p>
              <span className="font-medium text-gray-700">Duration:</span>{' '}
              {releaseConfig.durationDays} working days
            </p>
          )}
        </div>
        {releaseConfig.baseJql ? (
          <div className="mt-4 rounded-md border border-gray-100 bg-gray-50 px-4 py-3">
            <p className="text-xs font-medium uppercase tracking-wide text-gray-500">Base JQL</p>
            <p className="mt-1 font-mono text-xs text-gray-800">{releaseConfig.baseJql}</p>
          </div>
        ) : (
          <p className="mt-4 text-sm text-amber-700">
            This release has no base JQL configured. Edit the release to add one.
          </p>
        )}
      </section>

      <section className="rounded-xl border border-gray-200 bg-white p-5 shadow-sm">
        <div className="mb-4 flex items-center justify-between gap-3">
          <div>
            <h2 className="text-sm font-semibold text-gray-900">Additional JQL</h2>
            <p className="mt-1 text-sm text-gray-600">
              Optional. Combined with the release base JQL using AND.
            </p>
          </div>
          <button
            type="button"
            onClick={handleSearch}
            disabled={loading}
            className="btn-primary"
          >
            {loading ? 'Searching...' : 'Search issues'}
          </button>
        </div>
        <textarea
          value={draftAdditionalJql}
          onChange={(event) => setDraftAdditionalJql(event.target.value)}
          rows={3}
          placeholder='status = "In Progress"'
          className="w-full rounded-md border border-gray-300 px-3 py-2 font-mono text-sm text-gray-900 shadow-sm focus:border-brand-500 focus:outline-none focus:ring-2 focus:ring-brand-500"
        />
      </section>

      <div className="border-b border-gray-200">
        <nav className="-mb-px flex gap-6" aria-label="Release issue views">
          <ViewTabButton
            active={activeTab === 'issues'}
            onClick={() => setActiveTab('issues')}
            label="Issues"
          />
          <ViewTabButton
            active={activeTab === 'capacity'}
            onClick={() => setActiveTab('capacity')}
            label="Capacity"
          />
          <ViewTabButton
            active={activeTab === 'analysis'}
            onClick={() => setActiveTab('analysis')}
            label="Analysis"
          />
        </nav>
      </div>

      {activeTab === 'issues' && (
        <>
          {loading && <PageLoadingState message="Searching release issues..." />}

          {error && !loading && (
            <PageErrorState message={error} onRetry={() => void runSearch(0, appliedAdditionalJql)} />
          )}

          {page && !loading && !error && (
            <section className="space-y-4">
              <div className="flex items-center justify-between gap-3">
                <p className="text-sm text-gray-600">
                  Showing {page.issues.length} of {page.total} issues
                </p>
                <div className="flex gap-2">
                  <button
                    type="button"
                    onClick={() => void runSearch(Math.max(0, startAt - pageSize), appliedAdditionalJql)}
                    disabled={startAt === 0 || loading}
                    className="btn-secondary"
                  >
                    Previous
                  </button>
                  <button
                    type="button"
                    onClick={() => void runSearch(startAt + pageSize, appliedAdditionalJql)}
                    disabled={page.last || loading}
                    className="btn-secondary"
                  >
                    Next
                  </button>
                </div>
              </div>

              <IssueTable issues={page.issues} />
            </section>
          )}
        </>
      )}

      {activeTab === 'capacity' && (
        <div className="space-y-6">
          {appliedAdditionalJql.trim() && (
            <p className="rounded-md border border-brand-100 bg-brand-50 px-4 py-3 text-sm text-brand-800">
              Domain metrics reflect the additional JQL filter you last applied on the Issues tab.
            </p>
          )}

          {!releaseConfig.durationDays && (
            <p className="rounded-md border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800">
              Set the release duration (working days) when creating or editing the release to
              compute available capacity.
            </p>
          )}

          <ReleaseCapacityEditor
            initialCapacity={releaseConfig.capacity ?? []}
            initialLeavePercent={releaseConfig.leavePercent ?? 0}
            onSave={async (capacity, leavePercent) => {
              await updateReleaseCapacity(podId, releaseConfig.id, {
                capacity,
                leavePercent,
              })
              await handleCapacitySaved()
            }}
          />

          <section className="rounded-xl border border-gray-200 bg-white p-5 shadow-sm">
            <div className="mb-4 flex flex-wrap items-end justify-between gap-3">
              <div>
                <h2 className="text-sm font-semibold text-gray-900">Domain metrics</h2>
                <p className="mt-1 text-sm text-gray-600">
                  Committed story points are computed from release issues matching your current
                  filters.
                </p>
              </div>
              {capacityMetrics && !capacityLoading && (
                <div className="text-sm text-gray-600">
                  <span className="font-medium text-gray-800">Total capacity:</span>{' '}
                  {formatStoryPoints(capacityMetrics.totalAvailableCapacity)}
                  <span className="mx-2 text-gray-300">|</span>
                  <span className="font-medium text-gray-800">Committed SP:</span>{' '}
                  {formatStoryPoints(capacityMetrics.totalCommittedStoryPoints)}
                </div>
              )}
            </div>

            {capacityLoading && <PageLoadingState message="Computing domain metrics..." />}

            {capacityError && !capacityLoading && (
              <PageErrorState
                message={capacityError}
                onRetry={() => void loadCapacityMetrics(appliedAdditionalJql)}
              />
            )}

            {capacityMetrics && !capacityLoading && !capacityError && (
              <DomainMetricsTable metrics={capacityMetrics.domainMetrics} />
            )}
          </section>
        </div>
      )}

      {activeTab === 'analysis' && (
        <div className="space-y-6">
          {appliedAdditionalJql.trim() && (
            <p className="rounded-md border border-brand-100 bg-brand-50 px-4 py-3 text-sm text-brand-800">
              Analysis reflects the additional JQL filter you last applied on the Issues tab.
            </p>
          )}

          {analyticsLoading && <PageLoadingState message="Loading release analytics..." />}

          {analyticsError && !analyticsLoading && (
            <PageErrorState
              message={analyticsError}
              onRetry={() => void loadAnalytics(appliedAdditionalJql)}
            />
          )}

          {analytics && !analyticsLoading && !analyticsError && (
            <AnalyticsInsightsPanel analytics={analytics} />
          )}
        </div>
      )}
    </div>
  )
}

function ViewTabButton({
  active,
  onClick,
  label,
}: {
  active: boolean
  onClick: () => void
  label: string
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`border-b-2 px-1 py-3 text-sm font-medium transition ${
        active
          ? 'border-brand-600 text-brand-600'
          : 'border-transparent text-gray-500 hover:border-gray-200 hover:text-gray-600'
      }`}
    >
      {label}
    </button>
  )
}
