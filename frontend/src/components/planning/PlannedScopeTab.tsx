import { useCallback, useEffect, useState } from 'react'
import { RefreshCw, Save } from 'lucide-react'
import {
  ApiError,
  getPlannedIssues,
  getPlannedScope,
  updatePlannedScope,
} from '../../api'
import type { PlannedIssueViewDto, PlannedScopeDto } from '../../api/types'
import { PageErrorState, PageLoadingState } from '../common'
import { formatInstant } from '../../utils/format'
import { PlannedIssuesTable } from './PlannedIssuesTable'

interface PlannedScopeTabProps {
  podId: string
  jiraSprintId: number
  selectedIssueKeys: string[]
  onPlanningUpdated: () => Promise<void>
}

export function PlannedScopeTab({
  podId,
  jiraSprintId,
  selectedIssueKeys,
  onPlanningUpdated,
}: PlannedScopeTabProps) {
  const [scope, setScope] = useState<PlannedScopeDto | null>(null)
  const [plannedIssues, setPlannedIssues] = useState<PlannedIssueViewDto[]>([])
  const [draftKeys, setDraftKeys] = useState('')
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [saveMessage, setSaveMessage] = useState<string | null>(null)

  const loadData = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const [scopeData, issuesData] = await Promise.all([
        getPlannedScope(podId, jiraSprintId),
        getPlannedIssues(podId, jiraSprintId),
      ])
      setScope(scopeData)
      setPlannedIssues(issuesData)
      setDraftKeys((scopeData.plannedIssueKeys ?? []).join('\n'))
    } catch (err) {
      setScope(null)
      setPlannedIssues([])
      setError(err instanceof ApiError ? err.message : 'Failed to load planned scope.')
    } finally {
      setLoading(false)
    }
  }, [podId, jiraSprintId])

  useEffect(() => {
    void loadData()
  }, [loadData])

  const handleSave = async () => {
    const plannedIssueKeys = draftKeys
      .split(/[\n,]+/)
      .map((key) => key.trim())
      .filter(Boolean)

    setSaving(true)
    setSaveMessage(null)
    setError(null)
    try {
      const updated = await updatePlannedScope(podId, jiraSprintId, { plannedIssueKeys })
      setScope(updated)
      setDraftKeys((updated.plannedIssueKeys ?? []).join('\n'))
      await onPlanningUpdated()
      const issuesData = await getPlannedIssues(podId, jiraSprintId)
      setPlannedIssues(issuesData)
      setSaveMessage('Planned scope saved.')
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Failed to save planned scope.')
    } finally {
      setSaving(false)
    }
  }

  const handleUseSelected = () => {
    const merged = new Set([
      ...draftKeys
        .split(/[\n,]+/)
        .map((key) => key.trim())
        .filter(Boolean),
      ...selectedIssueKeys,
    ])
    setDraftKeys([...merged].join('\n'))
  }

  if (loading) {
    return <PageLoadingState message="Loading planned scope..." />
  }

  if (error && !scope) {
    return <PageErrorState message={error} onRetry={loadData} />
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="text-sm font-semibold text-gray-900">Planned scope</h2>
          <p className="mt-1 text-sm text-gray-600">
            Define which issues are in scope for this sprint plan.
          </p>
          {scope?.plannedScopeCapturedAt && (
            <p className="mt-1 text-xs text-gray-500">
              Last updated {formatInstant(scope.plannedScopeCapturedAt)}
            </p>
          )}
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
        <label htmlFor="planned-scope-keys" className="text-sm font-semibold text-gray-900">
          Planned issue keys
        </label>
        <p className="mt-1 text-sm text-gray-600">One key per line or comma-separated.</p>
        <textarea
          id="planned-scope-keys"
          value={draftKeys}
          onChange={(event) => setDraftKeys(event.target.value)}
          rows={6}
          className="mt-3 w-full rounded-md border border-gray-300 px-3 py-2 text-sm text-gray-900 shadow-sm focus:border-brand-500 focus:outline-none focus:ring-2 focus:ring-brand-500"
        />
        <div className="mt-4 flex flex-wrap gap-3">
          <button
            type="button"
            onClick={() => void handleSave()}
            disabled={saving}
            className="btn-primary"
          >
            <Save className="h-4 w-4" aria-hidden="true" />
            {saving ? 'Saving...' : 'Save scope'}
          </button>
          {selectedIssueKeys.length > 0 && (
            <button
              type="button"
              onClick={handleUseSelected}
              className="btn-secondary"
            >
              Add selected issues ({selectedIssueKeys.length})
            </button>
          )}
        </div>
        {saveMessage && <p className="mt-3 text-sm text-emerald-700">{saveMessage}</p>}
        {error && <p className="mt-3 text-sm text-red-600">{error}</p>}
      </section>

      <section className="rounded-xl border border-gray-200 bg-white p-5 shadow-sm">
        <h3 className="text-sm font-semibold text-gray-900">Planned issues (live Jira state)</h3>
        <div className="mt-4">
          <PlannedIssuesTable issues={plannedIssues} />
        </div>
      </section>
    </div>
  )
}
