import { Plus, Trash2 } from 'lucide-react'
import { useEffect, useState } from 'react'
import type { Domain, PersonCapacity } from '../../api/types'
import { DOMAIN_OPTIONS } from '../../utils/planningConstants'

interface CapacityEditorProps {
  initial: PersonCapacity[]
  onSave: (capacity: PersonCapacity[]) => Promise<void>
}

const emptyRow = (): PersonCapacity => ({
  personName: '',
  domain: 'DEV',
  bandwidthPercent: 100,
  velocity: 1,
})

export function CapacityEditor({ initial, onSave }: CapacityEditorProps) {
  const [rows, setRows] = useState<PersonCapacity[]>(initial)
  const [saving, setSaving] = useState(false)
  const [message, setMessage] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    setRows(initial)
  }, [initial])

  const updateRow = (index: number, patch: Partial<PersonCapacity>) => {
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
      setMessage('Capacity saved successfully.')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to save capacity.')
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
              <th className="px-4 py-3 text-left font-medium text-gray-600">Domain</th>
              <th className="px-4 py-3 text-right font-medium text-gray-600">Bandwidth %</th>
              <th className="px-4 py-3 text-right font-medium text-gray-600">Velocity</th>
              <th className="px-4 py-3 text-right font-medium text-gray-600">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100 bg-white">
            {rows.map((row, index) => (
              <tr key={`capacity-${index}`}>
                <td className="px-4 py-2">
                  <input
                    type="text"
                    value={row.personName}
                    onChange={(event) => updateRow(index, { personName: event.target.value })}
                    placeholder="Name"
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
                  <input
                    type="number"
                    min={0}
                    max={100}
                    step={0.1}
                    value={row.bandwidthPercent}
                    onChange={(event) =>
                      updateRow(index, { bandwidthPercent: Number(event.target.value) })
                    }
                    className="w-full rounded-md border border-gray-300 px-2 py-1.5 text-right text-sm"
                  />
                </td>
                <td className="px-4 py-2">
                  <input
                    type="number"
                    min={0.1}
                    step={0.1}
                    value={row.velocity ?? 1}
                    onChange={(event) =>
                      updateRow(index, { velocity: Number(event.target.value) })
                    }
                    className="w-full rounded-md border border-gray-300 px-2 py-1.5 text-right text-sm"
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
          {saving ? 'Saving...' : 'Save capacity'}
        </button>
      </div>

      {message && <p className="text-sm text-emerald-700">{message}</p>}
      {error && <p className="text-sm text-red-600">{error}</p>}
    </div>
  )
}
