import { useState, type FormEvent } from 'react'
import type { CreateReleaseRequest, ReleaseResponse, UpdateReleaseRequest } from '../../api/types'

export interface ReleaseFormState {
  name: string
  description: string
  baseJql: string
}

export const emptyReleaseForm: ReleaseFormState = {
  name: '',
  description: '',
  baseJql: '',
}

export function releaseToFormState(release: ReleaseResponse): ReleaseFormState {
  return {
    name: release.name,
    description: release.description ?? '',
    baseJql: release.baseJql ?? '',
  }
}

export function formStateToReleaseRequest(
  form: ReleaseFormState,
): CreateReleaseRequest | UpdateReleaseRequest {
  const baseJql = form.baseJql.trim()
  return {
    name: form.name.trim(),
    description: form.description.trim() || null,
    baseJql: baseJql || null,
  }
}

interface ReleaseFormProps {
  initial: ReleaseFormState
  submitLabel: string
  onSubmit: (request: CreateReleaseRequest | UpdateReleaseRequest) => Promise<void>
  onCancel: () => void
}

export function ReleaseForm({ initial, submitLabel, onSubmit, onCancel }: ReleaseFormProps) {
  const [form, setForm] = useState<ReleaseFormState>(initial)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const update = (field: keyof ReleaseFormState, value: string) => {
    setForm((current) => ({ ...current, [field]: value }))
  }

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault()
    if (!form.name.trim()) {
      setError('Release name is required.')
      return
    }
    if (!form.baseJql.trim()) {
      setError('Base JQL is required.')
      return
    }

    setSubmitting(true)
    setError(null)

    try {
      await onSubmit(formStateToReleaseRequest(form))
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to save release.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <div>
        <label htmlFor="release-name" className="block text-sm font-medium text-gray-700">
          Name
        </label>
        <input
          id="release-name"
          type="text"
          value={form.name}
          onChange={(event) => update('name', event.target.value)}
          className="mt-1.5 w-full rounded-md border border-gray-300 px-3 py-2 text-sm text-gray-900 shadow-sm focus:border-brand-500 focus:outline-none focus:ring-2 focus:ring-brand-500"
          required
        />
      </div>

      <div>
        <label htmlFor="release-description" className="block text-sm font-medium text-gray-700">
          Description
        </label>
        <textarea
          id="release-description"
          value={form.description}
          onChange={(event) => update('description', event.target.value)}
          rows={3}
          className="mt-1.5 w-full rounded-md border border-gray-300 px-3 py-2 text-sm text-gray-900 shadow-sm focus:border-brand-500 focus:outline-none focus:ring-2 focus:ring-brand-500"
        />
      </div>

      <div>
        <label htmlFor="release-base-jql" className="block text-sm font-medium text-gray-700">
          Base JQL
        </label>
        <p className="mt-1 text-xs text-gray-500">
          Defines the release issue scope. Additional filters on the Release Issues tab are combined
          with AND.
        </p>
        <textarea
          id="release-base-jql"
          value={form.baseJql}
          onChange={(event) => update('baseJql', event.target.value)}
          rows={4}
          placeholder='project = SCRUM AND fixVersion = "Q3"'
          className="mt-1.5 w-full rounded-md border border-gray-300 px-3 py-2 font-mono text-sm text-gray-900 shadow-sm focus:border-brand-500 focus:outline-none focus:ring-2 focus:ring-brand-500"
          required
        />
      </div>

      {error && <p className="text-sm text-red-600">{error}</p>}

      <div className="flex justify-end gap-3">
        <button type="button" onClick={onCancel} className="btn-secondary">
          Cancel
        </button>
        <button type="submit" disabled={submitting} className="btn-primary">
          {submitting ? 'Saving...' : submitLabel}
        </button>
      </div>
    </form>
  )
}
