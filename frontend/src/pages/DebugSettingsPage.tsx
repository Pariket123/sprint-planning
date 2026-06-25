import { PageHeader } from '../components/common'
import { useAppContext } from '../context/AppContext'

export function DebugSettingsPage() {
  const {
    selectedTeamId,
    selectedTeamName,
    selectedPodId,
    selectedPodName,
    selectedSprintId,
    clearAll,
  } = useAppContext()

  const storageEntries = [
    ['sprint-planning:teamId', localStorage.getItem('sprint-planning:teamId')],
    ['sprint-planning:teamName', localStorage.getItem('sprint-planning:teamName')],
    ['sprint-planning:podId', localStorage.getItem('sprint-planning:podId')],
    ['sprint-planning:podName', localStorage.getItem('sprint-planning:podName')],
    ['sprint-planning:sprintId', localStorage.getItem('sprint-planning:sprintId')],
  ] as const

  return (
    <div>
      <PageHeader
        title="Debug / Settings"
        description="Inspect persisted navigation context and frontend configuration."
        actions={
          <button
            type="button"
            onClick={clearAll}
            className="btn-secondary"
          >
            Clear context
          </button>
        }
      />

      <div className="grid gap-4 lg:grid-cols-2">
        <section className="rounded-xl border border-gray-200 bg-white p-5 shadow-sm">
          <h2 className="text-sm font-semibold text-gray-900">Selected context</h2>
          <dl className="mt-4 space-y-3 text-sm">
            <ContextRow label="Team ID" value={selectedTeamId} />
            <ContextRow label="Team name" value={selectedTeamName} />
            <ContextRow label="Pod ID" value={selectedPodId} />
            <ContextRow label="Pod name" value={selectedPodName} />
            <ContextRow
              label="Sprint ID"
              value={selectedSprintId === null ? null : String(selectedSprintId)}
            />
          </dl>
        </section>

        <section className="rounded-xl border border-gray-200 bg-white p-5 shadow-sm">
          <h2 className="text-sm font-semibold text-gray-900">Frontend config</h2>
          <dl className="mt-4 space-y-3 text-sm">
            <ContextRow label="API base URL" value="/api/v1 (proxied to localhost:8080 in dev)" />
            <ContextRow label="Environment" value={import.meta.env.MODE} />
          </dl>
        </section>

        <section className="rounded-xl border border-gray-200 bg-white p-5 shadow-sm lg:col-span-2">
          <h2 className="text-sm font-semibold text-gray-900">localStorage</h2>
          <dl className="mt-4 space-y-3 text-sm">
            {storageEntries.map(([key, value]) => (
              <ContextRow key={key} label={key} value={value} />
            ))}
          </dl>
        </section>
      </div>
    </div>
  )
}

function ContextRow({ label, value }: { label: string; value: string | null }) {
  return (
    <div className="grid gap-1 sm:grid-cols-[220px_1fr]">
      <dt className="font-medium text-gray-500">{label}</dt>
      <dd className="font-mono text-xs text-gray-900">{value ?? '-'}</dd>
    </div>
  )
}
