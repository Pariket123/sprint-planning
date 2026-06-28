import { useCallback, useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { ApiError, getSprintAnalytics, listSprints } from '../api'
import type { AnalyticsResponse, SprintView } from '../api/types'
import { AnalyticsInsightsPanel } from '../components/analytics/AnalyticsInsightsPanel'
import {
  PageEmptyState,
  PageErrorState,
  PageHeader,
  PageLoadingState,
} from '../components/common'
import { SprintSelector } from '../components/selectors/SprintSelector'
import { useAppContext } from '../context/AppContext'

export function AnalyzeSprintPage() {
  const { podId } = useParams<{ podId: string }>()
  const { selectedSprintId, setSprintId } = useAppContext()
  const [sprints, setSprints] = useState<SprintView[]>([])
  const [analytics, setAnalytics] = useState<AnalyticsResponse | null>(null)
  const [sprintsLoading, setSprintsLoading] = useState(true)
  const [analyticsLoading, setAnalyticsLoading] = useState(false)
  const [sprintsError, setSprintsError] = useState<string | null>(null)
  const [analyticsError, setAnalyticsError] = useState<string | null>(null)

  const loadSprints = useCallback(async () => {
    if (!podId) {
      setSprintsError('Pod is required.')
      setSprintsLoading(false)
      return
    }

    setSprintsLoading(true)
    setSprintsError(null)

    try {
      const sprintList = await listSprints(podId)
      setSprints(sprintList)
    } catch (err) {
      setSprints([])
      setSprintsError(err instanceof ApiError ? err.message : 'Failed to load sprints.')
    } finally {
      setSprintsLoading(false)
    }
  }, [podId])

  const loadAnalytics = useCallback(
    async (jiraSprintId: number) => {
      if (!podId) {
        return
      }

      setAnalyticsLoading(true)
      setAnalyticsError(null)

      try {
        const response = await getSprintAnalytics(podId, jiraSprintId)
        setAnalytics(response)
      } catch (err) {
        setAnalytics(null)
        setAnalyticsError(
          err instanceof ApiError ? err.message : 'Failed to load sprint analytics.',
        )
      } finally {
        setAnalyticsLoading(false)
      }
    },
    [podId],
  )

  useEffect(() => {
    void loadSprints()
  }, [loadSprints])

  useEffect(() => {
    if (sprints.length === 0) {
      return
    }

    const hasValidSelection =
      selectedSprintId !== null && sprints.some((sprint) => sprint.id === selectedSprintId)

    if (!hasValidSelection) {
      setSprintId(sprints[0].id)
    }
  }, [sprints, selectedSprintId, setSprintId])

  useEffect(() => {
    if (selectedSprintId !== null) {
      void loadAnalytics(selectedSprintId)
    } else {
      setAnalytics(null)
    }
  }, [selectedSprintId, loadAnalytics])

  const handleSprintChange = (sprintId: number) => {
    setSprintId(sprintId)
  }

  if (sprintsLoading) {
    return (
      <>
        <PageHeader
          title="Analyze Sprint"
          description="Review domain-wise story points, issue breakdown, and completion progress."
        />
        <PageLoadingState message="Loading sprints..." />
      </>
    )
  }

  if (sprintsError) {
    return (
      <>
        <PageHeader
          title="Analyze Sprint"
          description="Review domain-wise story points, issue breakdown, and completion progress."
        />
        <PageErrorState message={sprintsError} onRetry={loadSprints} />
      </>
    )
  }

  if (sprints.length === 0) {
    return (
      <>
        <PageHeader
          title="Analyze Sprint"
          description="Review domain-wise story points, issue breakdown, and completion progress."
        />
        <PageEmptyState
          title="No sprints found"
          description="Sprints from Jira will appear here when available for this pod."
        />
      </>
    )
  }

  return (
    <div className="space-y-6">
      <PageHeader
        title="Analyze Sprint"
        description="Review sprint story points, issue breakdown, and domain-wise analytics."
      />

      <SprintSelector
        sprints={sprints}
        selectedSprintId={selectedSprintId}
        onChange={handleSprintChange}
        description="Select a sprint to load live analytics from Jira."
      />

      {analyticsLoading && <PageLoadingState message="Loading analytics..." />}

      {analyticsError && !analyticsLoading && (
        <PageErrorState
          message={analyticsError}
          onRetry={() => {
            if (selectedSprintId !== null) {
              void loadAnalytics(selectedSprintId)
            }
          }}
        />
      )}

      {analytics && !analyticsLoading && !analyticsError && (
        <AnalyticsInsightsPanel analytics={analytics} />
      )}
    </div>
  )
}
