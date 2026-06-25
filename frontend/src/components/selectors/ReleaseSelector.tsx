import type { ReleaseResponse } from '../../api/types'

interface ReleaseSelectorProps {
  releases: ReleaseResponse[]
  selectedReleaseId: string | null
  onChange: (releaseId: string) => void
  loading?: boolean
}

export function ReleaseSelector({
  releases,
  selectedReleaseId,
  onChange,
  loading = false,
}: ReleaseSelectorProps) {
  return (
    <div>
      <label htmlFor="release-selector" className="block text-sm font-medium text-gray-900">
        Release
      </label>
      <p className="mt-1 text-sm text-gray-600">
        Select a release to search issues using its configured filters.
      </p>
      <select
        id="release-selector"
        value={selectedReleaseId ?? ''}
        onChange={(event) => onChange(event.target.value)}
        disabled={loading || releases.length === 0}
        className="mt-3 w-full max-w-xl rounded-md border border-gray-300 bg-white px-3 py-2 text-sm text-gray-900 shadow-sm focus:border-brand-500 focus:outline-none focus:ring-2 focus:ring-brand-500 disabled:cursor-not-allowed disabled:bg-gray-50"
      >
        <option value="" disabled>
          {loading ? 'Loading releases...' : 'Select a release'}
        </option>
        {releases.map((release) => (
          <option key={release.id} value={release.id}>
            {release.name}
          </option>
        ))}
      </select>
    </div>
  )
}
