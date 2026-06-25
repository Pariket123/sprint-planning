import { useCallback, useEffect, useState } from 'react'
import { RefreshCw } from 'lucide-react'
import {
  ApiError,
  getIncomingRollovers,
  getOutgoingRollovers,
  recordRollover,
} from '../../api'
import type { RolloverIssueDto, SprintView } from '../../api/types'
import { PageErrorState, PageLoadingState } from '../common'
import { formatDomain, formatInstant, formatStoryPoints } from '../../utils/format'

interface RolloverTabProps {
  podId: string
  jiraSprintId: number
  sprints: SprintView[]
  onPlanningUpdated: () => Promise<void>
}

export function RolloverTab({
  podId,
  jiraSprintId,
  sprints,
  onPlanningUpdated,
}: RolloverTabProps) {
  const [outgoing, setOutgoing] = useState<RolloverIssueDto[]>([])
  const [incoming, setIncoming] = useState<RolloverIssueDto[]>([])
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [formError, setFormError] = useState<string | null>(null)
  const [successMessage, setSuccessMessage] = useState<string | null>(null)

  const [issueKey, setIssueKey] = useState('')
  const [toSprintId, setToSprintId] = useState<number | ''>('')
  const [notes, setNotes] = useState('')
  const [moveInJira, setMoveInJira] = useState(false)

  const loadRollovers = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const [outgoingData, incomingData] = await Promise.all([
        getOutgoingRollovers(podId, jiraSprintId),
        getIncomingRollovers(podId, jiraSprintId),
      ])
      setOutgoing(outgoingData)
      setIncoming(incomingData)
    } catch (err) {
      setOutgoing([])
      setIncoming([])
      setError(err instanceof ApiError ? err.message : 'Failed to load rollover records.')
    } finally {
      setLoading(false)
    }
  }, [podId, jiraSprintId])

  useEffect(() => {
    void loadRollovers()
  }, [loadRollovers])

  const handleRecord = async () => {
    if (!issueKey.trim() || toSprintId === '') {
      setFormError('Issue key and destination sprint are required.')
      return
    }

    setSubmitting(true)
    setFormError(null)
    setSuccessMessage(null)
    try {
      await recordRollover(podId, jiraSprintId, {
        issueKey: issueKey.trim(),
        toSprintId,
        notes: notes.trim() || null,
        moveInJira,
      })
      setIssueKey('')
      setNotes('')
      setSuccessMessage('Rollover recorded.')
      await onPlanningUpdated()
      await loadRollovers()
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
    return <PageErrorState message={error} onRetry={loadRollovers} />
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="text-sm font-semibold text-gray-900">Rollover</h2>
          <p className="mt-1 text-sm text-gray-600">
            Track issues rolling out of or into this sprint.
          </p>
        </div>
        <button
          type="button"
          onClick={() => void loadRollovers()}
          className="btn-secondary"
        >
          <RefreshCw className="h-4 w-4" aria-hidden="true" />
          Refresh
        </button>
      </div>

      <section className="rounded-xl border border-gray-200 bg-white p-5 shadow-sm">
        <h3 className="text-sm font-semibold text-gray-900">Record rollover</h3>
        <div className="mt-4 grid gap-4 sm:grid-cols-2">
          <div>
            <label htmlFor="rollover-issue-key" className="text-sm font-medium text-gray-700">
              Issue key
            </label>
            <input
              id="rollover-issue-key"
              value={issueKey}
              onChange={(event) => setIssueKey(event.target.value)}
              placeholder="SCRUM-1"
              className="mt-1 w-full rounded-md border border-gray-300 px-3 py-2 text-sm"
            />
          </div>
          <div>
            <label htmlFor="rollover-to-sprint" className="text-sm font-medium text-gray-700">
              To sprint
            </label>
            <select
              id="rollover-to-sprint"
              value={toSprintId}
              onChange={(event) =>
                setToSprintId(event.target.value === '' ? '' : Number(event.target.value))
              }
              className="mt-1 w-full rounded-md border border-gray-300 px-3 py-2 text-sm"
            >
              <option value="">Select sprint</option>
              {sprints
                .filter((sprint) => sprint.id !== jiraSprintId)
                .map((sprint) => (
                  <option key={sprint.id} value={sprint.id}>
                    {sprint.name}
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
            disabled={submitting}
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

      <RolloverTable title="Outgoing rollovers" records={outgoing} />
      <RolloverTable title="Incoming rollovers" records={incoming} />
    </div>
  )
}

function RolloverTable({ title, records }: { title: string; records: RolloverIssueDto[] }) {
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
                  <td className="px-4 py-3 text-gray-700">{record.fromSprintId}</td>
                  <td className="px-4 py-3 text-gray-700">{record.toSprintId}</td>
                  <td className="px-4 py-3 text-gray-700">{record.statusAtRollover ?? '-'}</td>
                  <td className="px-4 py-3 text-right text-gray-700">
                    {formatStoryPoints(record.storyPointsAtRollover)}
                  </td>
                  <td className="px-4 py-3 text-gray-700">{formatDomain(record.domain)}</td>
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
