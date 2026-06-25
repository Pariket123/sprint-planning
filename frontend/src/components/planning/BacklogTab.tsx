import { useCallback, useEffect, useState } from 'react'
import { ArrowRight, RefreshCw } from 'lucide-react'
import { ApiError, getBacklog, moveIssuesToSprint } from '../../api'
import type { BacklogPageDto } from '../../api/types'
import { PageErrorState, PageLoadingState } from '../common'
import { PlanningIssueTable } from './PlanningIssueTable'

interface BacklogTabProps {
  podId: string
  jiraSprintId: number
  onPlanningUpdated: () => Promise<void>
}

const PAGE_SIZE = 50

export function BacklogTab({ podId, jiraSprintId, onPlanningUpdated }: BacklogTabProps) {
  const [page, setPage] = useState<BacklogPageDto | null>(null)
  const [startAt, setStartAt] = useState(0)
  const [loading, setLoading] = useState(true)
  const [moving, setMoving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)
  const [selectedKeys, setSelectedKeys] = useState<string[]>([])
  const [addToPlannedScope, setAddToPlannedScope] = useState(true)

  const loadBacklog = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await getBacklog(podId, startAt, PAGE_SIZE)
      setPage(data)
      setSelectedKeys([])
    } catch (err) {
      setPage(null)
      setError(err instanceof ApiError ? err.message : 'Failed to load backlog.')
    } finally {
      setLoading(false)
    }
  }, [podId, startAt])

  useEffect(() => {
    void loadBacklog()
  }, [loadBacklog])

  const handleMoveToSprint = async () => {
    if (selectedKeys.length === 0) {
      return
    }

    setMoving(true)
    setActionError(null)
    try {
      await moveIssuesToSprint(podId, jiraSprintId, {
        issueKeys: selectedKeys,
        addToPlannedScope,
      })
      await onPlanningUpdated()
      await loadBacklog()
    } catch (err) {
      setActionError(err instanceof ApiError ? err.message : 'Failed to move issues to sprint.')
    } finally {
      setMoving(false)
    }
  }

  if (loading) {
    return <PageLoadingState message="Loading backlog..." />
  }

  if (error) {
    return <PageErrorState message={error} onRetry={loadBacklog} />
  }

  const issues = page?.issues ?? []

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="text-sm font-semibold text-gray-900">Pod backlog</h2>
          <p className="mt-1 text-sm text-gray-600">
            Select issues to move into the current sprint.
          </p>
        </div>
        <button
          type="button"
          onClick={() => void loadBacklog()}
          className="btn-secondary"
        >
          <RefreshCw className="h-4 w-4" aria-hidden="true" />
          Refresh
        </button>
      </div>

      <div className="flex flex-wrap items-center gap-4 rounded-lg border border-gray-200 bg-gray-50 px-4 py-3">
        <label className="inline-flex items-center gap-2 text-sm text-gray-700">
          <input
            type="checkbox"
            checked={addToPlannedScope}
            onChange={(event) => setAddToPlannedScope(event.target.checked)}
          />
          Add to planned scope
        </label>
        <button
          type="button"
          onClick={() => void handleMoveToSprint()}
          disabled={moving || selectedKeys.length === 0}
          className="btn-primary"
        >
          <ArrowRight className="h-4 w-4" aria-hidden="true" />
          {moving ? 'Moving...' : `Move to sprint (${selectedKeys.length})`}
        </button>
      </div>

      {actionError && (
        <p className="text-sm text-red-600" role="alert">
          {actionError}
        </p>
      )}

      <PlanningIssueTable
        issues={issues}
        selectable
        selectedKeys={selectedKeys}
        onSelectionChange={setSelectedKeys}
      />

      {page && (
        <PaginationBar
          startAt={page.startAt}
          maxResults={page.maxResults}
          total={page.total}
          last={page.last}
          onPrevious={() => setStartAt(Math.max(0, page.startAt - page.maxResults))}
          onNext={() => setStartAt(page.startAt + page.maxResults)}
        />
      )}
    </div>
  )
}

function PaginationBar({
  startAt,
  maxResults,
  total,
  last,
  onPrevious,
  onNext,
}: {
  startAt: number
  maxResults: number
  total: number
  last: boolean
  onPrevious: () => void
  onNext: () => void
}) {
  const from = total === 0 ? 0 : startAt + 1
  const to = Math.min(startAt + maxResults, total)

  return (
    <div className="flex items-center justify-between gap-3 text-sm text-gray-600">
      <span>
        Showing {from}–{to} of {total}
      </span>
      <div className="flex gap-2">
        <button
          type="button"
          onClick={onPrevious}
          disabled={startAt === 0}
          className="btn-secondary"
        >
          Previous
        </button>
        <button
          type="button"
          onClick={onNext}
          disabled={last}
          className="btn-secondary"
        >
          Next
        </button>
      </div>
    </div>
  )
}
