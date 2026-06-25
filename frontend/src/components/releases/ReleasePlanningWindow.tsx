import { useEffect, useState } from 'react'
import type { ReleaseResponse } from '../../api/types'

interface ReleasePlanningWindowProps {
  release: ReleaseResponse
  onSave: (durationDays: number, startDate: string | null) => Promise<void>
}

export function ReleasePlanningWindow({ release, onSave }: ReleasePlanningWindowProps) {
  const [durationDays, setDurationDays] = useState(
    release.durationDays != null ? String(release.durationDays) : '',
  )
  const [startDate, setStartDate] = useState(release.startDate ?? '')
  const [saving, setSaving] = useState(false)
  const [message, setMessage] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    setDurationDays(release.durationDays != null ? String(release.durationDays) : '')
    setStartDate(release.startDate ?? '')
  }, [release.durationDays, release.startDate])

  const handleSave = async () => {
    const parsedDuration = Number(durationDays)
    if (!Number.isFinite(parsedDuration) || parsedDuration < 1) {
      setError('Duration must be at least 1 working day.')
      return
    }

    setSaving(true)
    setError(null)
    setMessage(null)
    try {
      await onSave(parsedDuration, startDate.trim() || null)
      setMessage('Planning window saved.')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to save planning window.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="space-y-4">
      <div className="grid gap-4 sm:grid-cols-2">
        <div>
          <label htmlFor="release-start-date" className="block text-sm font-medium text-gray-700">
            Start date
          </label>
          <p className="mt-1 text-xs text-gray-500">Required for leave and holiday deductions.</p>
          <input
            id="release-start-date"
            type="date"
            value={startDate}
            onChange={(event) => setStartDate(event.target.value)}
            className="mt-1.5 w-full rounded-md border border-gray-300 px-3 py-2 text-sm text-gray-900 shadow-sm focus:border-brand-500 focus:outline-none focus:ring-2 focus:ring-brand-500"
          />
        </div>
        <div>
          <label htmlFor="release-duration-days" className="block text-sm font-medium text-gray-700">
            Duration (working days)
          </label>
          <p className="mt-1 text-xs text-gray-500">
            Used with bandwidth % to compute available capacity per domain.
          </p>
          <input
            id="release-duration-days"
            type="number"
            min={1}
            step={1}
            value={durationDays}
            onChange={(event) => setDurationDays(event.target.value)}
            className="mt-1.5 w-full rounded-md border border-gray-300 px-3 py-2 text-sm text-gray-900 shadow-sm focus:border-brand-500 focus:outline-none focus:ring-2 focus:ring-brand-500"
            required
          />
        </div>
      </div>

      <div className="flex flex-wrap items-center gap-3">
        <button
          type="button"
          onClick={() => void handleSave()}
          disabled={saving}
          className="btn-primary"
        >
          {saving ? 'Saving...' : 'Save planning window'}
        </button>
      </div>

      {message && <p className="text-sm text-emerald-700">{message}</p>}
      {error && <p className="text-sm text-red-600">{error}</p>}
    </div>
  )
}
