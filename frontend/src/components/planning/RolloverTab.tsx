import { useCallback, useEffect, useMemo, useState } from 'react'
import { RefreshCw } from 'lucide-react'
import {
  ApiError,
  getIncomingRollovers,
  getOutgoingRollovers,
  getPlanningIssues,
  recordRollover,
} from '../../api'
import type { IssueView, RolloverIssueDto, SprintView } from '../../api/types'
import { PageErrorState, PageLoadingState } from '../common'
import {
  formatInstant,
  formatIssueDomain,
  formatSprintState,
  formatStoryPoints,
} from '../../utils/format'

interface RolloverTabProps {
  podId: string
  jiraSprintId: number
  sprints: SprintView[]
  onPlanningUpdated: () => Promise<void>
}

function isIncomplete(issue: IssueView): boolean {
  return issue.statusCategory !== 'DONE'
}

function sortDestinationSprints(sprints: SprintView[], fromSprintId: number): SprintView[] {
  const stateOrder = (state: string) => {
    const normalized = state.toLowerCase()
    if (normalized === 'future') {
      return 0
    }
    if (normalized === 'active') {
      return 1
    }
    return 2
  }

  return sprints
    .filter((sprint) => sprint.id !== fromSprintId)
    .filter((sprint) => {
      const state = sprint.state.toLowerCase()
      return state === 'future' || state === 'active'
    })
    .sort((left, right) => {
      const byState = stateOrder(left.state) - stateOrder(right.state)
      if (byState !== 0) {
        return byState
      }
      const leftStart = left.startDate ? Date.parse(left.startDate) : Number.MAX_SAFE_INTEGER
      const rightStart = right.startDate ? Date.parse(right.startDate) : Number.MAX_SAFE_INTEGER
      return leftStart - rightStart
    })
}

function defaultDestinationSprintId(
  destinationSprints: SprintView[],
  currentSprint: SprintView | undefined,
): number | '' {
  if (destinationSprints.length === 0) {
    return ''
  }

  const futureSprints = destinationSprints.filter((sprint) => sprint.state.toLowerCase() === 'future')
  if (futureSprints.length === 0) {
    return destinationSprints[0].id
  }

  if (currentSprint?.endDate) {
    const currentEnd = Date.parse(currentSprint.endDate)
    const nextSprint = futureSprints.find((sprint) => {
      if (!sprint.startDate) {
        return false
      }
      return Date.parse(sprint.startDate) >= currentEnd
    })
    if (nextSprint) {
      return nextSprint.id
    }
  }

  return futureSprints[0].id
}

export function RolloverTab({
  podId,
  jiraSprintId,
  sprints,
  onPlanningUpdated,
}: RolloverTabProps) {
  const [outgoing, setOutgoing] = useState<RolloverIssueDto[]>([])
  const [incoming, setIncoming] = useState<RolloverIssueDto[]>([])
  const [sprintIssues, setSprintIssues] = useState<IssueView[]>([])
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [formError, setFormError] = useState<string | null>(null)
  const [successMessage, setSuccessMessage] = useState<string | null>(null)

  const [issueKey, setIssueKey] = useState('')
  const [toSprintId, setToSprintId] = useState<number | ''>('')
  const [notes, setNotes] = useState('')
  const [moveInJira, setMoveInJira] = useState(true)
  const [showCompletedIssues, setShowCompletedIssues] = useState(false)

  const currentSprint = sprints.find((sprint) => sprint.id === jiraSprintId)
  const destinationSprints = useMemo(
    () => sortDestinationSprints(sprints, jiraSprintId),
    [sprints, jiraSprintId],
  )

  const alreadyRolledOutKeys = useMemo(
    () => new Set(outgoing.map((record) => record.issueKey)),
    [outgoing],
  )

  const selectableIssues = useMemo(() => {
    return sprintIssues.filter((issue) => {
      if (alreadyRolledOutKeys.has(issue.key)) {
        return false
      }
      if (!showCompletedIssues && !isIncomplete(issue)) {
        return false
      }
      return true
    })
  }, [sprintIssues, alreadyRolledOutKeys, showCompletedIssues])

  const loadData = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const [outgoingData, incomingData, issuesPage] = await Promise.all([
        getOutgoingRollovers(podId, jiraSprintId),
        getIncomingRollovers(podId, jiraSprintId),
        getPlanningIssues(podId, jiraSprintId, 0, 100),
      ])
      setOutgoing(outgoingData)
      setIncoming(incomingData)
      setSprintIssues(issuesPage.sprintIssues ?? [])
    } catch (err) {
      setOutgoing([])
      setIncoming([])
      setSprintIssues([])
      setError(err instanceof ApiError ? err.message : 'Failed to load rollover data.')
    } finally {
      setLoading(false)
    }
  }, [podId, jiraSprintId])

  useEffect(() => {
    void loadData()
  }, [loadData])

  useEffect(() => {
    setToSprintId(defaultDestinationSprintId(destinationSprints, currentSprint))
  }, [destinationSprints, currentSprint])

  useEffect(() => {
    if (issueKey && !selectableIssues.some((issue) => issue.key === issueKey)) {
      setIssueKey('')
    }
  }, [issueKey, selectableIssues])

  const handleRecord = async () => {
    if (!issueKey || toSprintId === '') {
      setFormError('Select an issue from this sprint and a destination sprint.')
      return
    }

    setSubmitting(true)
    setFormError(null)
    setSuccessMessage(null)
    try {
      await recordRollover(podId, jiraSprintId, {
        issueKey,
        toSprintId,
        notes: notes.trim() || null,
        moveInJira,
      })
      setIssueKey('')
      setNotes('')
      setSuccessMessage('Rollover recorded.')
      await onPlanningUpdated()
      await loadData()
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : 'Failed to record rollover.')
    } finally {
      setSubmitting(false)
    }
  }

  if (loading) {
    return <PageLoadingState message="Loading rollover records..." />
  }

  if (error) {
    return <PageErrorState message={error} onRetry={loadData} />
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="text-sm font-semibold text-gray-900">Rollover</h2>
          <p className="mt-1 text-sm text-gray-600">
            Move incomplete work from this sprint into the next sprint and track what rolled over.
          </p>
        </div>
        <button
          type="button"
          onClick={() => void loadData()}
          className="btn-secondary"
        >
          <RefreshCw className="h-4 w-4" aria-hidden="true" />
          Refresh
        </button>
      </div>

      <section className="rounded-xl border border-gray-200 bg-white p-5 shadow-sm">
        <h3 className="text-sm font-semibold text-gray-900">Record rollover</h3>
        <p className="mt-1 text-sm text-gray-600">
          Select an issue from {currentSprint?.name ?? 'this sprint'} that will not be completed,
          then choose the sprint it should roll into.
        </p>
        <div className="mt-4 grid gap-4 sm:grid-cols-2">
          <div>
            <label htmlFor="rollover-issue-key" className="text-sm font-medium text-gray-700">
              Issue in this sprint
            </label>
            <select
              id="rollover-issue-key"
              value={issueKey}
              onChange={(event) => setIssueKey(event.target.value)}
              className="mt-1 w-full rounded-md border border-gray-300 px-3 py-2 text-sm"
            >
              <option value="">
                {selectableIssues.length === 0
                  ? 'No eligible issues in this sprint'
                  : 'Select issue'}
              </option>
              {selectableIssues.map((issue) => (
                <option key={issue.key} value={issue.key}>
                  {issue.key} — {issue.summary} ({issue.status}, {formatStoryPoints(issue.storyPoints)} SP)
                </option>
              ))}
            </select>
            <label className="mt-2 inline-flex items-center gap-2 text-xs text-gray-600">
              <input
                type="checkbox"
                checked={showCompletedIssues}
                onChange={(event) => setShowCompletedIssues(event.target.checked)}
              />
              Include completed issues
            </label>
          </div>
          <div>
            <label htmlFor="rollover-to-sprint" className="text-sm font-medium text-gray-700">
              Roll into sprint
            </label>
            <select
              id="rollover-to-sprint"
              value={toSprintId}
              onChange={(event) =>
                setToSprintId(event.target.value === '' ? '' : Number(event.target.value))
              }
              className="mt-1 w-full rounded-md border border-gray-300 px-3 py-2 text-sm"
            >
              <option value="">
                {destinationSprints.length === 0
                  ? 'No active or future sprints available'
                  : 'Select sprint'}
              </option>
              {destinationSprints.map((sprint) => (
                <option key={sprint.id} value={sprint.id}>
                  {sprint.name} ({formatSprintState(sprint.state)})
                </option>
              ))}
            </select>
          </div>
          <div className="sm:col-span-2">
            <label htmlFor="rollover-notes" className="text-sm font-medium text-gray-700">
              Notes
            </label>
            <input
              id="rollover-notes"
              value={notes}
              onChange={(event) => setNotes(event.target.value)}
              placeholder="Optional reason or context"
              className="mt-1 w-full rounded-md border border-gray-300 px-3 py-2 text-sm"
            />
          </div>
        </div>
        <label className="mt-4 inline-flex items-center gap-2 text-sm text-gray-700">
          <input
            type="checkbox"
            checked={moveInJira}
            onChange={(event) => setMoveInJira(event.target.checked)}
          />
          Move issue in Jira
        </label>
        <div className="mt-4">
          <button
            type="button"
            onClick={() => void handleRecord()}
            disabled={submitting || !issueKey || toSprintId === ''}
            className="btn-primary"
          >
            {submitting ? 'Recording...' : 'Record rollover'}
          </button>
        </div>
        {formError && (
          <p className="mt-3 text-sm text-red-600" role="alert">
            {formError}
          </p>
        )}
        {successMessage && <p className="mt-3 text-sm text-emerald-700">{successMessage}</p>}
      </section>

      <RolloverTable title="Outgoing rollovers" records={outgoing} sprints={sprints} />
      <RolloverTable title="Incoming rollovers" records={incoming} sprints={sprints} />
    </div>
  )
}

function RolloverTable({
  title,
  records,
  sprints,
}: {
  title: string
  records: RolloverIssueDto[]
  sprints: SprintView[]
}) {
  const sprintNameById = useMemo(
    () => new Map(sprints.map((sprint) => [sprint.id, sprint.name])),
    [sprints],
  )

  const sprintLabel = (sprintId: number) => sprintNameById.get(sprintId) ?? String(sprintId)

  return (
    <section className="rounded-xl border border-gray-200 bg-white p-5 shadow-sm">
      <h3 className="text-sm font-semibold text-gray-900">{title}</h3>
      {records.length === 0 ? (
        <p className="mt-3 text-sm text-gray-500">No records.</p>
      ) : (
        <div className="mt-4 overflow-x-auto rounded-lg border border-gray-200">
          <table className="min-w-full divide-y divide-gray-200 text-sm">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-4 py-3 text-left font-medium text-gray-600">Issue</th>
                <th className="px-4 py-3 text-left font-medium text-gray-600">From</th>
                <th className="px-4 py-3 text-left font-medium text-gray-600">To</th>
                <th className="px-4 py-3 text-left font-medium text-gray-600">Status</th>
                <th className="px-4 py-3 text-right font-medium text-gray-600">SP</th>
                <th className="px-4 py-3 text-left font-medium text-gray-600">Domain</th>
                <th className="px-4 py-3 text-left font-medium text-gray-600">Recorded</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100 bg-white">
              {records.map((record) => (
                <tr key={`${record.issueKey}-${record.fromSprintId}-${record.toSprintId}`}>
                  <td className="px-4 py-3 font-medium text-brand-600">{record.issueKey}</td>
                  <td className="px-4 py-3 text-gray-700">{sprintLabel(record.fromSprintId)}</td>
                  <td className="px-4 py-3 text-gray-700">{sprintLabel(record.toSprintId)}</td>
                  <td className="px-4 py-3 text-gray-700">{record.statusAtRollover ?? '-'}</td>
                  <td className="px-4 py-3 text-right text-gray-700">
                    {formatStoryPoints(record.storyPointsAtRollover)}
                  </td>
                  <td className="px-4 py-3 text-gray-700">
                    {formatIssueDomain(record.domain, record.domainLabel)}
                  </td>
                  <td className="px-4 py-3 text-gray-700">
                    {formatInstant(record.rolledOverAt)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  )
}
