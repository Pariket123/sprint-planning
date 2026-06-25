import { Plus, Trash2 } from 'lucide-react'
import { useEffect, useState } from 'react'
import type { Domain, LeaveEntry, LeaveType } from '../../api/types'
import { DOMAIN_OPTIONS, LEAVE_TYPE_OPTIONS } from '../../utils/planningConstants'

interface LeavesEditorProps {
  initial: LeaveEntry[]
  onSave: (leaves: LeaveEntry[]) => Promise<void>
}

const emptyRow = (): LeaveEntry => ({
  personName: '',
  startDate: '',
  endDate: '',
  domain: 'DEV',
  type: 'LEAVE',
})

export function LeavesEditor({ initial, onSave }: LeavesEditorProps) {
  const [rows, setRows] = useState<LeaveEntry[]>(initial)
  const [saving, setSaving] = useState(false)
  const [message, setMessage] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    setRows(initial)
  }, [initial])

  const updateRow = (index: number, patch: Partial<LeaveEntry>) => {
    setRows((current) =>
      current.map((row, rowIndex) => (rowIndex === index ? { ...row, ...patch } : row)),
    )
  }

  const handleSave = async () => {
    setSaving(true)
    setError(null)
    setMessage(null)
    try {
      await onSave(rows)
      setMessage('Leaves and holidays saved successfully.')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to save leaves.')
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
              <th className="px-4 py-3 text-left font-medium text-gray-600">Person</th>
              <th className="px-4 py-3 text-left font-medium text-gray-600">Start date</th>
              <th className="px-4 py-3 text-left font-medium text-gray-600">End date</th>
              <th className="px-4 py-3 text-left font-medium text-gray-600">Domain</th>
              <th className="px-4 py-3 text-left font-medium text-gray-600">Type</th>
              <th className="px-4 py-3 text-right font-medium text-gray-600">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100 bg-white">
            {rows.map((row, index) => (
              <tr key={`leave-${index}`}>
                <td className="px-4 py-2">
                  <input
                    type="text"
                    value={row.personName}
                    onChange={(event) => updateRow(index, { personName: event.target.value })}
                    placeholder={row.type === 'HOLIDAY' ? 'Optional for team holiday' : 'Name'}
                    className="w-full rounded-md border border-gray-300 px-2 py-1.5 text-sm"
                  />
                </td>
                <td className="px-4 py-2">
                  <input
                    type="date"
                    value={row.startDate}
                    onChange={(event) => updateRow(index, { startDate: event.target.value })}
                    className="w-full rounded-md border border-gray-300 px-2 py-1.5 text-sm"
                  />
                </td>
                <td className="px-4 py-2">
                  <input
                    type="date"
                    value={row.endDate}
                    onChange={(event) => updateRow(index, { endDate: event.target.value })}
                    className="w-full rounded-md border border-gray-300 px-2 py-1.5 text-sm"
                  />
                </td>
                <td className="px-4 py-2">
                  <select
                    value={row.domain}
                    onChange={(event) =>
                      updateRow(index, { domain: event.target.value as Domain })
                    }
                    className="w-full rounded-md border border-gray-300 px-2 py-1.5 text-sm"
                  >
                    {DOMAIN_OPTIONS.map((domain) => (
                      <option key={domain} value={domain}>
                        {domain}
                      </option>
                    ))}
                  </select>
                </td>
                <td className="px-4 py-2">
                  <select
                    value={row.type}
                    onChange={(event) =>
                      updateRow(index, { type: event.target.value as LeaveType })
                    }
                    className="w-full rounded-md border border-gray-300 px-2 py-1.5 text-sm"
                  >
                    {LEAVE_TYPE_OPTIONS.map((type) => (
                      <option key={type} value={type}>
                        {type}
                      </option>
                    ))}
                  </select>
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

      <p className="text-xs text-gray-500">
        Team holidays (type HOLIDAY) reduce working days for everyone. Personal leave (type LEAVE)
        deducts capacity from the person&apos;s domain using their bandwidth percentage.
      </p>

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
          {saving ? 'Saving...' : 'Save leaves'}
        </button>
      </div>

      {message && <p className="text-sm text-emerald-700">{message}</p>}
      {error && <p className="text-sm text-red-600">{error}</p>}
    </div>
  )
}
