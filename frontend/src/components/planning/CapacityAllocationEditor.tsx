import { useEffect, useMemo, useState } from 'react'
import type {
  CapacityAllocationPercents,
  CapacityAllocationTableDto,
} from '../../api/types'
import { formatStoryPoints } from '../../utils/format'

interface CapacityAllocationEditorProps {
  table: CapacityAllocationTableDto | null | undefined
  initialPercents: CapacityAllocationPercents[]
  onSave: (capacityAllocation: CapacityAllocationPercents[]) => Promise<void>
}

function buildDraftPercents(
  table: CapacityAllocationTableDto | null | undefined,
  initialPercents: CapacityAllocationPercents[],
): CapacityAllocationPercents[] {
  const savedByKey = new Map(initialPercents.map((entry) => [entry.key.toUpperCase(), entry]))
  const rows = table?.rows ?? []
  if (rows.length === 0) {
    return initialPercents
  }
  return rows.map((row) => {
    const saved = savedByKey.get(row.key.toUpperCase())
    return {
      key: row.key,
      roadmapPercent: saved?.roadmapPercent ?? row.roadmapPercent,
      bugSupportPercent: saved?.bugSupportPercent ?? row.bugSupportPercent,
    }
  })
}

function computePlanned(available: number, percent: number): number {
  return Math.round(available * (percent / 100) * 100) / 100
}

export function CapacityAllocationEditor({
  table,
  initialPercents,
  onSave,
}: CapacityAllocationEditorProps) {
  const [draft, setDraft] = useState<CapacityAllocationPercents[]>(() =>
    buildDraftPercents(table, initialPercents),
  )
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [message, setMessage] = useState<string | null>(null)

  useEffect(() => {
    setDraft(buildDraftPercents(table, initialPercents))
  }, [table, initialPercents])

  const rows = table?.rows ?? []
  const draftByKey = useMemo(
    () => new Map(draft.map((entry) => [entry.key.toUpperCase(), entry])),
    [draft],
  )

  const updatePercent = (key: string, field: 'roadmapPercent' | 'bugSupportPercent', value: number) => {
    setDraft((current) =>
      current.map((entry) =>
        entry.key.toUpperCase() === key.toUpperCase() ? { ...entry, [field]: value } : entry,
      ),
    )
  }

  const handleSave = async () => {
    setSaving(true)
    setError(null)
    setMessage(null)
    try {
      await onSave(draft)
      setMessage('Capacity allocation saved successfully.')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to save capacity allocation.')
    } finally {
      setSaving(false)
    }
  }

  if (rows.length === 0) {
    return (
      <p className="text-sm text-gray-500">
        Add team capacity for BE, UI, AI, or QA to configure roadmap vs bug/support split.
      </p>
    )
  }

  return (
    <div className="space-y-4">
      <div className="overflow-x-auto rounded-lg border border-gray-200">
        <table className="min-w-full divide-y divide-gray-200 text-sm">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-4 py-3 text-left font-medium text-gray-600">Domain</th>
              <th className="px-4 py-3 text-right font-medium text-gray-600">Available (SPs)</th>
              <th className="px-4 py-3 text-right font-medium text-gray-600">% Roadmap</th>
              <th className="px-4 py-3 text-right font-medium text-gray-600">% Bug / support</th>
              <th className="px-4 py-3 text-right font-medium text-gray-600">Planned roadmap SPs</th>
              <th className="px-4 py-3 text-right font-medium text-gray-600">Planned bug/support SPs</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100 bg-white">
            {rows.map((row) => {
              const percents = draftByKey.get(row.key.toUpperCase())
              const roadmapPercent = percents?.roadmapPercent ?? row.roadmapPercent
              const bugSupportPercent = percents?.bugSupportPercent ?? row.bugSupportPercent
              return (
                <tr key={row.key}>
                  <td className="px-4 py-3 font-medium text-gray-900">{row.label}</td>
                  <td className="px-4 py-3 text-right text-gray-700">
                    {formatStoryPoints(row.availableStoryPoints)}
                  </td>
                  <td className="px-4 py-3 text-right">
                    <input
                      type="number"
                      min={0}
                      max={100}
                      step={1}
                      value={roadmapPercent}
                      onChange={(event) =>
                        updatePercent(row.key, 'roadmapPercent', Number(event.target.value))
                      }
                      className="w-20 rounded-md border border-gray-300 px-2 py-1.5 text-right text-sm"
                    />
                  </td>
                  <td className="px-4 py-3 text-right">
                    <input
                      type="number"
                      min={0}
                      max={100}
                      step={1}
                      value={bugSupportPercent}
                      onChange={(event) =>
                        updatePercent(row.key, 'bugSupportPercent', Number(event.target.value))
                      }
                      className="w-20 rounded-md border border-gray-300 px-2 py-1.5 text-right text-sm"
                    />
                  </td>
                  <td className="px-4 py-3 text-right text-gray-700">
                    {formatStoryPoints(computePlanned(row.availableStoryPoints, roadmapPercent))}
                  </td>
                  <td className="px-4 py-3 text-right text-gray-700">
                    {formatStoryPoints(
                      computePlanned(row.availableStoryPoints, bugSupportPercent),
                    )}
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>

      <p className="text-xs text-gray-500">
        Utilization in domain metrics uses planned roadmap capacity (second-to-last column).
      </p>

      <div className="flex items-center gap-3">
        <button type="button" onClick={() => void handleSave()} disabled={saving} className="btn-primary">
          {saving ? 'Saving...' : 'Save capacity allocation'}
        </button>
        {message && <p className="text-sm text-green-700">{message}</p>}
        {error && <p className="text-sm text-red-600">{error}</p>}
      </div>
    </div>
  )
}
