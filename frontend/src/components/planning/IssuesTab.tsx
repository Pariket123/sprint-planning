import { useState } from 'react'
import { ArrowLeft, ArrowRight } from 'lucide-react'
import { ApiError, moveIssuesToBacklog, moveIssuesToSprint } from '../../api'
import type { PlanningViewDto } from '../../api/types'
import { PlanningIssueTable } from './PlanningIssueTable'

interface IssuesTabProps {
  podId: string
  jiraSprintId: number
  planning: PlanningViewDto
  onPlanningUpdated: () => Promise<void>
}

export function IssuesTab({ podId, jiraSprintId, planning, onPlanningUpdated }: IssuesTabProps) {
  const [sprintSelected, setSprintSelected] = useState<string[]>([])
  const [moving, setMoving] = useState(false)
  const [actionError, setActionError] = useState<string | null>(null)
  const [addToPlannedScope, setAddToPlannedScope] = useState(true)

  const sprintIssues = planning.sprintIssues ?? []
  const selectedIssues = planning.selectedIssues ?? []

  const handleMoveToBacklog = async () => {
    if (sprintSelected.length === 0) {
      return
    }

    setMoving(true)
    setActionError(null)
    try {
      await moveIssuesToBacklog(podId, { issueKeys: sprintSelected })
      setSprintSelected([])
      await onPlanningUpdated()
    } catch (err) {
      setActionError(err instanceof ApiError ? err.message : 'Failed to move issues to backlog.')
    } finally {
      setMoving(false)
    }
  }

  const handleCommit = async () => {
    if (sprintSelected.length === 0) {
      return
    }

    setMoving(true)
    setActionError(null)
    try {
      await moveIssuesToSprint(podId, jiraSprintId, {
        issueKeys: sprintSelected,
        addToPlannedScope,
      })
      setSprintSelected([])
      await onPlanningUpdated()
    } catch (err) {
      setActionError(err instanceof ApiError ? err.message : 'Failed to commit issues.')
    } finally {
      setMoving(false)
    }
  }

  return (
    <div className="space-y-6">
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
          disabled={moving || sprintSelected.length === 0}
          className="btn-primary"
        >
          <ArrowRight className="h-4 w-4" aria-hidden="true" />
          {moving ? 'Working...' : `Commit selected (${sprintSelected.length})`}
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
          title="Sprint issues"
          selectable
          selectedKeys={sprintSelected}
          onSelectionChange={setSprintSelected}
        />
      </section>

      <section className="rounded-xl border border-gray-200 bg-white p-5 shadow-sm">
        <PlanningIssueTable issues={selectedIssues} title="Selected issues (planning view)" />
      </section>

      {(planning.committedIssueKeys ?? []).length > 0 && (
        <section className="rounded-xl border border-gray-200 bg-white p-5 shadow-sm">
          <h3 className="text-sm font-semibold text-gray-900">Committed issue keys</h3>
          <p className="mt-2 text-sm text-gray-600">{(planning.committedIssueKeys ?? []).join(', ')}</p>
        </section>
      )}

      {(planning.plannedIssueKeys ?? []).length > 0 && (
        <section className="rounded-xl border border-gray-200 bg-white p-5 shadow-sm">
          <h3 className="text-sm font-semibold text-gray-900">Planned issue keys</h3>
          <p className="mt-2 text-sm text-gray-600">{(planning.plannedIssueKeys ?? []).join(', ')}</p>
        </section>
      )}
    </div>
  )
}
