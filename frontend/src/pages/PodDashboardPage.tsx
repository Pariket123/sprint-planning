import { useCallback, useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { BarChart3, CalendarRange, Package } from 'lucide-react'
import { ApiError, listActiveAndFutureSprints, listReleases } from '../api'
import type { ReleaseResponse, SprintView } from '../api/types'
import {
  PageErrorState,
  PageHeader,
  PageLoadingState,
  StatusBadge,
} from '../components/common'
import {
  ActionCard,
  formatDomainValues,
  formatList,
  SummaryItem,
} from '../components/dashboard/DashboardCards'
import { usePodRouteContext } from '../routes/guards/podRouteContext'

interface DashboardCounts {
  sprints: SprintView[]
  releases: ReleaseResponse[]
  sprintLoadError: string | null
  releaseLoadError: string | null
}

export function PodDashboardPage() {
  const { podId } = useParams<{ podId: string }>()
  const { pod } = usePodRouteContext()
  const [counts, setCounts] = useState<DashboardCounts | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const loadCounts = useCallback(async () => {
    if (!podId) {
      setError('Pod is required.')
      setLoading(false)
      return
    }

    setLoading(true)
    setError(null)

    try {
      const [sprintsResult, releasesResult] = await Promise.allSettled([
        listActiveAndFutureSprints(podId),
        listReleases(podId),
      ])

      setCounts({
        sprints: sprintsResult.status === 'fulfilled' ? sprintsResult.value : [],
        releases: releasesResult.status === 'fulfilled' ? releasesResult.value : [],
        sprintLoadError:
          sprintsResult.status === 'rejected'
            ? sprintsResult.reason instanceof ApiError
              ? sprintsResult.reason.message
              : 'Failed to load sprints.'
            : null,
        releaseLoadError:
          releasesResult.status === 'rejected'
            ? releasesResult.reason instanceof ApiError
              ? releasesResult.reason.message
              : 'Failed to load releases.'
            : null,
      })
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Failed to load pod dashboard.')
      setCounts(null)
    } finally {
      setLoading(false)
    }
  }, [podId])

  useEffect(() => {
    void loadCounts()
  }, [loadCounts])

  if (loading) {
    return (
      <>
        <PageHeader
          title="Pod Dashboard"
          description="Review pod configuration and choose a planning workflow."
        />
        <PageLoadingState message="Loading pod dashboard..." />
      </>
    )
  }

  if (error) {
    return (
      <>
        <PageHeader
          title="Pod Dashboard"
          description="Review pod configuration and choose a planning workflow."
        />
        <PageErrorState message={error} onRetry={loadCounts} />
      </>
    )
  }

  const sprints = counts?.sprints ?? []
  const releases = counts?.releases ?? []
  const sprintLoadError = counts?.sprintLoadError ?? null
  const releaseLoadError = counts?.releaseLoadError ?? null
  const jiraConfig = pod.jiraConfig
  const activeReleaseCount = releases.filter((release) => release.active).length
  const activeSprintCount = sprints.filter((sprint) => sprint.state === 'active').length
  const futureSprintCount = sprints.filter((sprint) => sprint.state === 'future').length

  return (
    <div className="space-y-6">
      <PageHeader
        title={pod.name}
        description="Choose a workflow to analyze sprints, plan capacity, or manage releases."
        actions={<StatusBadge active={pod.active} />}
      />

      <section className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
        <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h2 className="text-sm font-semibold text-gray-900">Pod summary</h2>
            <p className="mt-1 text-sm text-gray-600">
              Configuration and quick counts for this module.
            </p>
          </div>
          {pod.code && (
            <span className="inline-flex w-fit rounded-md bg-gray-100 px-2.5 py-1 text-xs font-medium text-gray-700">
              {pod.code}
            </span>
          )}
        </div>

        <dl className="mt-5 grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
          <SummaryItem label="Jira project keys" value={formatList(jiraConfig?.projectKeys)} />
          <SummaryItem label="Board ID" value={jiraConfig?.boardId ?? '-'} />
          <SummaryItem label="Story points field" value={jiraConfig?.storyPointsField ?? '-'} />
          <SummaryItem label="Domain field" value={jiraConfig?.domainField ?? '-'} />
          <SummaryItem label="Domain values" value={formatDomainValues(jiraConfig?.domainValues)} />
          <SummaryItem
            label="Active sprints"
            value={sprintLoadError ? 'Unavailable' : activeSprintCount}
          />
          <SummaryItem
            label="Future sprints"
            value={sprintLoadError ? 'Unavailable' : futureSprintCount}
          />
          <SummaryItem
            label="Active releases"
            value={releaseLoadError ? 'Unavailable' : activeReleaseCount}
          />
        </dl>

        {(sprintLoadError || releaseLoadError) && (
          <div className="mt-4 rounded-md border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800">
            {sprintLoadError && <p>Sprints: {sprintLoadError}</p>}
            {releaseLoadError && (
              <p className={sprintLoadError ? 'mt-1' : ''}>Releases: {releaseLoadError}</p>
            )}
          </div>
        )}
      </section>

      <section>
        <div className="mb-4">
          <h2 className="text-sm font-semibold text-gray-900">Planning workflows</h2>
          <p className="mt-1 text-sm text-gray-600">
            Primary actions available inside this pod.
          </p>
        </div>

        <div className="grid gap-4 lg:grid-cols-3">
          <ActionCard
            title="Analyze Sprint"
            description="Review sprint story points, issue breakdown, status distribution, and domain-wise analytics."
            to={`/pods/${podId}/analyze`}
            icon={BarChart3}
          />
          <ActionCard
            title="Plan Sprint"
            description="Manage capacity, issues, planned scope, commits, backlog moves, and rollover."
            to={`/pods/${podId}/plan`}
            icon={CalendarRange}
          />
          <ActionCard
            title="Create/View Release"
            description="Configure team release filters and search release issues."
            to={`/pods/${podId}/releases`}
            icon={Package}
          />
        </div>
      </section>
    </div>
  )
}
