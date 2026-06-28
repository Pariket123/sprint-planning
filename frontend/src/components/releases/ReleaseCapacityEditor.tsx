import { Plus, Trash2 } from 'lucide-react'
import { useEffect, useState } from 'react'
import type { Domain, PersonCapacity } from '../../api/types'
import { DOMAIN_OPTIONS } from '../../utils/planningConstants'

interface ReleaseCapacityEditorProps {
  initialCapacity: PersonCapacity[]
  initialLeavePercent: number
  onSave: (capacity: PersonCapacity[], leavePercent: number) => Promise<void>
}

const emptyRow = (): PersonCapacity => ({
  personName: '',
  domain: 'DEV',
  bandwidthPercent: 100,
})

export function ReleaseCapacityEditor({
  initialCapacity,
  initialLeavePercent,
  onSave,
}: ReleaseCapacityEditorProps) {
  const [rows, setRows] = useState<PersonCapacity[]>(initialCapacity)
  const [leavePercent, setLeavePercent] = useState(initialLeavePercent)
  const [savingCapacity, setSavingCapacity] = useState(false)
  const [savingLeave, setSavingLeave] = useState(false)
  const [capacityMessage, setCapacityMessage] = useState<string | null>(null)
  const [capacityError, setCapacityError] = useState<string | null>(null)
  const [leaveMessage, setLeaveMessage] = useState<string | null>(null)
  const [leaveError, setLeaveError] = useState<string | null>(null)

  useEffect(() => {
    setRows(initialCapacity)
    setLeavePercent(initialLeavePercent)
  }, [initialCapacity, initialLeavePercent])

  const updateRow = (index: number, patch: Partial<PersonCapacity>) => {
    setRows((current) =>
      current.map((row, rowIndex) => (rowIndex === index ? { ...row, ...patch } : row)),
    )
  }

  const normalizeLeavePercent = (value: number) =>
    Number.isFinite(value) ? Math.min(100, Math.max(0, value)) : 0

  const persist = async (
    successMessage: string,
    setSaving: (value: boolean) => void,
    setMessage: (value: string | null) => void,
    setError: (value: string | null) => void,
  ) => {
    setSaving(true)
    setError(null)
    setMessage(null)
    try {
      await onSave(rows, normalizeLeavePercent(leavePercent))
      setMessage(successMessage)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to save.')
    } finally {
      setSaving(false)
    }
  }

  const handleSaveCapacity = () =>
    void persist(
      'Capacity saved successfully.',
      setSavingCapacity,
      setCapacityMessage,
      setCapacityError,
    )

  const handleSaveLeave = () =>
    void persist(
      'Leave settings saved successfully.',
      setSavingLeave,
      setLeaveMessage,
      setLeaveError,
    )

  return (
    <div className="space-y-6">
      <section className="rounded-xl border border-gray-200 bg-white p-5 shadow-sm">
        <h2 className="text-sm font-semibold text-gray-900">Capacity</h2>
        <p className="mt-1 text-sm text-gray-600">
          Add each team member with their domain and bandwidth for this release window.
        </p>

        <div className="mt-4 overflow-x-auto rounded-lg border border-gray-200">
          <table className="min-w-full divide-y divide-gray-200 text-sm">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-4 py-3 text-left font-medium text-gray-600">Person</th>
                <th className="px-4 py-3 text-left font-medium text-gray-600">Domain</th>
                <th className="px-4 py-3 text-right font-medium text-gray-600">Bandwidth %</th>
                <th className="px-4 py-3 text-right font-medium text-gray-600">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100 bg-white">
              {rows.length === 0 ? (
                <tr>
                  <td colSpan={4} className="px-4 py-6 text-center text-sm text-gray-500">
                    No team members added yet. Use &quot;Add row&quot; to define release capacity.
                  </td>
                </tr>
              ) : (
                rows.map((row, index) => (
                  <tr key={`release-capacity-${index}`}>
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
                ))
              )}
            </tbody>
          </table>
        </div>

        <div className="mt-4 flex flex-wrap items-center gap-3">
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
            onClick={handleSaveCapacity}
            disabled={savingCapacity || savingLeave}
            className="btn-primary"
          >
            {savingCapacity ? 'Saving...' : 'Save capacity'}
          </button>
        </div>

        {capacityMessage && <p className="mt-3 text-sm text-emerald-700">{capacityMessage}</p>}
        {capacityError && <p className="mt-3 text-sm text-red-600">{capacityError}</p>}
      </section>

      <section className="rounded-xl border border-gray-200 bg-white p-5 shadow-sm">
        <h2 className="text-sm font-semibold text-gray-900">Leave adjustment</h2>
        <p className="mt-1 text-sm text-gray-600">
          Reduces effective capacity for every domain. Example: 10% leave on 400 capacity = 360.
        </p>

        <div className="mt-4 max-w-xs">
          <label htmlFor="release-leave-percent" className="block text-sm font-medium text-gray-700">
            Leave %
          </label>
          <input
            id="release-leave-percent"
            type="number"
            min={0}
            max={100}
            step={0.1}
            value={leavePercent}
            onChange={(event) => setLeavePercent(Number(event.target.value))}
            className="mt-1.5 w-full rounded-md border border-gray-300 px-3 py-2 text-sm text-gray-900 shadow-sm focus:border-brand-500 focus:outline-none focus:ring-2 focus:ring-brand-500"
          />
        </div>

        <div className="mt-4">
          <button
            type="button"
            onClick={handleSaveLeave}
            disabled={savingCapacity || savingLeave}
            className="btn-primary"
          >
            {savingLeave ? 'Saving...' : 'Save leave'}
          </button>
        </div>

        {leaveMessage && <p className="mt-3 text-sm text-emerald-700">{leaveMessage}</p>}
        {leaveError && <p className="mt-3 text-sm text-red-600">{leaveError}</p>}
      </section>
    </div>
  )
}
