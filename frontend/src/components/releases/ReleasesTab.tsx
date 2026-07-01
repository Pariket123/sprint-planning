import { useCallback, useEffect, useState } from 'react'
import { Pencil, Plus, Trash2 } from 'lucide-react'
import {
  ApiError,
  createRelease,
  deactivateRelease,
  listReleases,
  updateRelease,
} from '../../api'
import type { CreateReleaseRequest, ReleaseResponse, UpdateReleaseRequest } from '../../api/types'
import { ConfirmDialog } from '../common/ConfirmDialog'
import { PageEmptyState, PageErrorState, PageLoadingState } from '../common'
import {
  emptyReleaseForm,
  ReleaseForm,
  releaseToFormState,
} from './ReleaseForm'
import { formatInstant } from '../../utils/format'

interface ReleasesTabProps {
  podId: string
  onReleasesChange: (releases: ReleaseResponse[]) => void
  onViewIssues: (releaseId: string) => void
}

export function ReleasesTab({ podId, onReleasesChange, onViewIssues }: ReleasesTabProps) {
  const [releases, setReleases] = useState<ReleaseResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [mode, setMode] = useState<'list' | 'create' | 'edit'>('list')
  const [editingRelease, setEditingRelease] = useState<ReleaseResponse | null>(null)
  const [deleteTarget, setDeleteTarget] = useState<ReleaseResponse | null>(null)
  const [deleting, setDeleting] = useState(false)

  const loadReleases = useCallback(async () => {
    setLoading(true)
    setError(null)

    try {
      const data = await listReleases(podId)
      setReleases(data)
      onReleasesChange(data)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Failed to load releases.')
    } finally {
      setLoading(false)
    }
  }, [podId, onReleasesChange])

  useEffect(() => {
    void loadReleases()
  }, [loadReleases])

  const handleCreate = async (request: CreateReleaseRequest | UpdateReleaseRequest) => {
    await createRelease(podId, request as CreateReleaseRequest)
    setMode('list')
    await loadReleases()
  }

  const handleUpdate = async (request: CreateReleaseRequest | UpdateReleaseRequest) => {
    if (!editingRelease) {
      return
    }
    await updateRelease(podId, editingRelease.id, request as UpdateReleaseRequest)
    setMode('list')
    setEditingRelease(null)
    await loadReleases()
  }

  const handleDelete = async () => {
    if (!deleteTarget) {
      return
    }

    setDeleting(true)
    try {
      await deactivateRelease(podId, deleteTarget.id)
      setDeleteTarget(null)
      await loadReleases()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Failed to deactivate release.')
      setDeleteTarget(null)
    } finally {
      setDeleting(false)
    }
  }

  if (loading) {
    return <PageLoadingState message="Loading releases..." />
  }

  if (error && releases.length === 0 && mode === 'list') {
    return <PageErrorState message={error} onRetry={loadReleases} />
  }

  if (mode === 'create') {
    return (
      <section className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
        <h2 className="text-lg font-semibold text-gray-900">Create release</h2>
        <div className="mt-4">
          <ReleaseForm
            podId={podId}
            initial={emptyReleaseForm}
            submitLabel="Create release"
            onSubmit={handleCreate}
            onCancel={() => setMode('list')}
          />
        </div>
      </section>
    )
  }

  if (mode === 'edit' && editingRelease) {
    return (
      <section className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
        <h2 className="text-lg font-semibold text-gray-900">Edit release</h2>
        <div className="mt-4">
          <ReleaseForm
            podId={podId}
            initial={releaseToFormState(editingRelease)}
            submitLabel="Save changes"
            onSubmit={handleUpdate}
            onCancel={() => {
              setMode('list')
              setEditingRelease(null)
            }}
          />
        </div>
      </section>
    )
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between gap-3">
        <div>
          <h2 className="text-sm font-semibold text-gray-900">Releases</h2>
          <p className="mt-1 text-sm text-gray-600">
            Create and manage release configurations for this pod.
          </p>
        </div>
        <button
          type="button"
          onClick={() => setMode('create')}
          className="btn-primary"
        >
          <Plus className="h-4 w-4" aria-hidden="true" />
          Create release
        </button>
      </div>

      {error && (
        <div className="rounded-md border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {error}
        </div>
      )}

      {releases.length === 0 ? (
        <PageEmptyState
          title="No releases yet"
          description="Create a release with a base JQL query to define its issue scope."
          action={
            <button
              type="button"
              onClick={() => setMode('create')}
              className="btn-primary"
            >
              Create release
            </button>
          }
        />
      ) : (
        <div className="overflow-x-auto rounded-lg border border-gray-200">
          <table className="min-w-full divide-y divide-gray-200 text-sm">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-4 py-3 text-left font-medium text-gray-600">Name</th>
                <th className="px-4 py-3 text-left font-medium text-gray-600">Base JQL</th>
                <th className="px-4 py-3 text-left font-medium text-gray-600">Updated</th>
                <th className="px-4 py-3 text-right font-medium text-gray-600">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100 bg-white">
              {releases.map((release) => (
                <tr key={release.id}>
                  <td className="cursor-pointer px-4 py-3">
                    <button
                      type="button"
                      onClick={() => onViewIssues(release.id)}
                      className="text-left font-medium text-brand-600 hover:underline"
                    >
                      {release.name}
                    </button>
                    {release.description && (
                      <p className="mt-0.5 text-xs text-gray-500">{release.description}</p>
                    )}
                  </td>
                  <td className="max-w-md truncate px-4 py-3 font-mono text-xs text-gray-700">
                    {release.baseJql ?? '-'}
                  </td>
                  <td className="px-4 py-3 text-gray-700">
                    {formatInstant(release.updatedAt)}
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex justify-end gap-2">
                      <button
                        type="button"
                        onClick={() => {
                          setEditingRelease(release)
                          setMode('edit')
                        }}
                        className="btn-secondary gap-1 px-2.5 py-1 text-xs"
                      >
                        <Pencil className="h-3.5 w-3.5" aria-hidden="true" />
                        Edit
                      </button>
                      <button
                        type="button"
                        onClick={() => setDeleteTarget(release)}
                        className="inline-flex items-center gap-1 rounded-full border border-red-200 bg-red-50 px-2.5 py-1 text-xs font-medium text-red-700 hover:bg-red-100"
                      >
                        <Trash2 className="h-3.5 w-3.5" aria-hidden="true" />
                        Deactivate
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <ConfirmDialog
        open={deleteTarget !== null}
        title="Deactivate release?"
        message={`Deactivate "${deleteTarget?.name}"? It will no longer appear in active release lists.`}
        confirmLabel="Deactivate"
        onConfirm={() => void handleDelete()}
        onCancel={() => setDeleteTarget(null)}
        loading={deleting}
      />
    </div>
  )
}
