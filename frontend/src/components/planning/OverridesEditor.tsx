import { Plus, Trash2 } from 'lucide-react'
import { useEffect, useState } from 'react'
import type { OverrideAction, PlanningOverride } from '../../api/types'
import { OVERRIDE_ACTION_OPTIONS } from '../../utils/planningConstants'

interface OverridesEditorProps {
  initial: PlanningOverride[]
  onSave: (overrides: PlanningOverride[]) => Promise<void>
}

const emptyRow = (): PlanningOverride => ({
  issueKey: '',
  action: 'EXCLUDE',
  notes: null,
})

export function OverridesEditor({ initial, onSave }: OverridesEditorProps) {
  const [rows, setRows] = useState<PlanningOverride[]>(initial)
  const [saving, setSaving] = useState(false)
  const [message, setMessage] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    setRows(initial)
  }, [initial])

  const updateRow = (index: number, patch: Partial<PlanningOverride>) => {
    setRows((current) =>
      current.map((row, rowIndex) => (rowIndex === index ? { ...row, ...patch } : row)),
    )
  }

  const handleSave = async () => {
    setSaving(true)
    setError(null)
    setMessage(null)
    try {
      await onSave(rows.filter((row) => row.issueKey.trim().length > 0))
      setMessage('Overrides saved successfully.')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to save overrides.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="space-y-4">
      <div className="overflow-x-auto rounded-lg border border-gray-200">
        <table className="min-w-full divide-y divide-gray-200 text-sm">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-4 py-3 text-left font-medium text-gray-600">Issue key</th>
              <th className="px-4 py-3 text-left font-medium text-gray-600">Action</th>
              <th className="px-4 py-3 text-left font-medium text-gray-600">Notes</th>
              <th className="px-4 py-3 text-right font-medium text-gray-600">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100 bg-white">
            {rows.map((row, index) => (
              <tr key={`override-${index}`}>
                <td className="px-4 py-2">
                  <input
                    type="text"
                    value={row.issueKey}
                    onChange={(event) => updateRow(index, { issueKey: event.target.value })}
                    placeholder="PROJ-123"
                    className="w-full rounded-md border border-gray-300 px-2 py-1.5 text-sm"
                  />
                </td>
                <td className="px-4 py-2">
                  <select
                    value={row.action}
                    onChange={(event) =>
                      updateRow(index, { action: event.target.value as OverrideAction })
                    }
                    className="w-full rounded-md border border-gray-300 px-2 py-1.5 text-sm"
                  >
                    {OVERRIDE_ACTION_OPTIONS.map((action) => (
                      <option key={action} value={action}>
                        {action}
                      </option>
                    ))}
                  </select>
                </td>
                <td className="px-4 py-2">
                  <input
                    type="text"
                    value={row.notes ?? ''}
                    onChange={(event) =>
                      updateRow(index, { notes: event.target.value || null })
                    }
                    className="w-full rounded-md border border-gray-300 px-2 py-1.5 text-sm"
                  />
                </td>
                <td className="px-4 py-2 text-right">
                  <button
                    type="button"
                    onClick={() => setRows((current) => current.filter((_, i) => i !== index))}
                    className="inline-flex items-center gap-1 rounded-md border border-gray-300 px-2 py-1 text-xs text-gray-700 hover:bg-gray-50"
                  >
                    <Trash2 className="h-3.5 w-3.5" aria-hidden="true" />
                    Remove
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="flex flex-wrap items-center gap-3">
        <button
          type="button"
          onClick={() => setRows((current) => [...current, emptyRow()])}
          className="btn-secondary"
        >
          <Plus className="h-4 w-4" aria-hidden="true" />
          Add row
        </button>
        <button
          type="button"
          onClick={() => void handleSave()}
          disabled={saving}
          className="btn-primary"
        >
          {saving ? 'Saving...' : 'Save overrides'}
        </button>
      </div>

      {message && <p className="text-sm text-emerald-700">{message}</p>}
      {error && <p className="text-sm text-red-600">{error}</p>}
    </div>
  )
}
