import { useCallback, useEffect, useState } from 'react'
import { ArrowLeft, ArrowRight, MinusCircle } from 'lucide-react'
import {
  ApiError,
  getPlanningIssues,
  moveIssuesToBacklog,
  moveIssuesToSprint,
  uncommitIssues,
} from '../../api'
import type { PlanningIssuesPageDto, PlanningViewDto } from '../../api/types'
import { LoadingState } from '../common'
import { PlanningIssueTable } from './PlanningIssueTable'

interface IssuesTabProps {
  podId: string
  jiraSprintId: number
  planning: PlanningViewDto
  onPlanningUpdated: () => Promise<void>
}

export function IssuesTab({ podId, jiraSprintId, planning, onPlanningUpdated }: IssuesTabProps) {
  const [issuesPage, setIssuesPage] = useState<PlanningIssuesPageDto | null>(null)
  const [issuesLoading, setIssuesLoading] = useState(true)
  const [issuesError, setIssuesError] = useState<string | null>(null)
  const [sprintSelected, setSprintSelected] = useState<string[]>([])
  const [moving, setMoving] = useState(false)
  const [actionError, setActionError] = useState<string | null>(null)
  const [addToPlannedScope, setAddToPlannedScope] = useState(true)

  const committedKeys = planning.committedIssueKeys ?? []

  const loadIssues = useCallback(async () => {
    setIssuesLoading(true)
    setIssuesError(null)

    try {
      const page = await getPlanningIssues(podId, jiraSprintId)
      setIssuesPage(page)
    } catch (err) {
      setIssuesPage(null)
      setIssuesError(err instanceof ApiError ? err.message : 'Failed to load sprint issues.')
    } finally {
      setIssuesLoading(false)
    }
  }, [podId, jiraSprintId])

  useEffect(() => {
    void loadIssues()
  }, [loadIssues])

  const sprintIssues = issuesPage?.sprintIssues ?? []
  const selectedIssues = issuesPage?.selectedIssues ?? []
  const selectedCommittedKeys = sprintSelected.filter((key) => committedKeys.includes(key))
  const selectedUncommittedKeys = sprintSelected.filter((key) => !committedKeys.includes(key))

  const handleMoveToBacklog = async () => {
    if (sprintSelected.length === 0) {
      return
    }

    setMoving(true)
    setActionError(null)
    try {
      await moveIssuesToBacklog(podId, jiraSprintId, { issueKeys: sprintSelected })
      setSprintSelected([])
      await onPlanningUpdated()
      await loadIssues()
    } catch (err) {
      setActionError(err instanceof ApiError ? err.message : 'Failed to move issues to backlog.')
    } finally {
      setMoving(false)
    }
  }

  const handleCommit = async () => {
    if (selectedUncommittedKeys.length === 0) {
      return
    }

    setMoving(true)
    setActionError(null)
    try {
      await moveIssuesToSprint(podId, jiraSprintId, {
        issueKeys: selectedUncommittedKeys,
        addToPlannedScope,
      })
      setSprintSelected([])
      await onPlanningUpdated()
      await loadIssues()
    } catch (err) {
      setActionError(err instanceof ApiError ? err.message : 'Failed to commit issues.')
    } finally {
      setMoving(false)
    }
  }

  const handleUncommit = async () => {
    if (selectedCommittedKeys.length === 0) {
      return
    }

    setMoving(true)
    setActionError(null)
    try {
      await uncommitIssues(podId, jiraSprintId, { issueKeys: selectedCommittedKeys })
      setSprintSelected([])
      await onPlanningUpdated()
      await loadIssues()
    } catch (err) {
      setActionError(err instanceof ApiError ? err.message : 'Failed to uncommit issues.')
    } finally {
      setMoving(false)
    }
  }

  if (issuesLoading) {
    return <LoadingState message="Loading sprint issues..." />
  }

  if (issuesError) {
    return (
      <p className="text-sm text-red-600" role="alert">
        {issuesError}
      </p>
    )
  }

  return (
    <div className="space-y-6">
      <div className="rounded-lg border border-blue-200 bg-blue-50 px-4 py-3 text-sm text-blue-900">
        <p className="font-medium">Reduce over-capacity</p>
        <p className="mt-1">
          Utilization follows <span className="font-medium">committed</span> story points. Select
          committed issues and use <span className="font-medium">Uncommit from plan</span> to lower
          utilization while keeping them in the Jira sprint. Use{' '}
          <span className="font-medium">Move to backlog</span> to remove them from the sprint
          entirely.
        </p>
      </div>

      <div className="flex flex-wrap items-center gap-4 rounded-lg border border-gray-200 bg-gray-50 px-4 py-3">
        <label className="inline-flex items-center gap-2 text-sm text-gray-700">
          <input
            type="checkbox"
            checked={addToPlannedScope}
            onChange={(event) => setAddToPlannedScope(event.target.checked)}
          />
          Add to planned scope when committing
        </label>
        <button
          type="button"
          onClick={() => void handleUncommit()}
          disabled={moving || selectedCommittedKeys.length === 0}
          className="btn-secondary"
        >
          <MinusCircle className="h-4 w-4" aria-hidden="true" />
          {moving
            ? 'Working...'
            : `Uncommit from plan (${selectedCommittedKeys.length})`}
        </button>
        <button
          type="button"
          onClick={() => void handleMoveToBacklog()}
          disabled={moving || sprintSelected.length === 0}
          className="btn-secondary"
        >
          <ArrowLeft className="h-4 w-4" aria-hidden="true" />
          {moving ? 'Working...' : `Move to backlog (${sprintSelected.length})`}
        </button>
        <button
          type="button"
          onClick={() => void handleCommit()}
          disabled={moving || selectedUncommittedKeys.length === 0}
          className="btn-primary"
        >
          <ArrowRight className="h-4 w-4" aria-hidden="true" />
          {moving ? 'Working...' : `Commit selected (${selectedUncommittedKeys.length})`}
        </button>
      </div>

      {actionError && (
        <p className="text-sm text-red-600" role="alert">
          {actionError}
        </p>
      )}

      <section className="rounded-xl border border-gray-200 bg-white p-5 shadow-sm">
        <PlanningIssueTable
          issues={sprintIssues}
          title={`Sprint issues (${issuesPage?.sprintIssueTotal ?? sprintIssues.length})`}
          selectable
          selectedKeys={sprintSelected}
          committedKeys={committedKeys}
          onSelectionChange={setSprintSelected}
        />
      </section>

      <section className="rounded-xl border border-gray-200 bg-white p-5 shadow-sm">
        <PlanningIssueTable
          issues={selectedIssues}
          title={`Selected issues (${planning.selectedIssueCount ?? selectedIssues.length})`}
          committedKeys={committedKeys}
        />
      </section>
    </div>
  )
}
