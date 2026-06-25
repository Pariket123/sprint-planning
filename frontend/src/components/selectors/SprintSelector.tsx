import { CalendarDays, Flag } from 'lucide-react'
import type { ReactNode } from 'react'
import type { SprintView } from '../../api/types'
import { formatInstant, formatSprintState } from '../../utils/format'

interface SprintSelectorProps {
  sprints: SprintView[]
  selectedSprintId: number | null
  onChange: (sprintId: number) => void
  loading?: boolean
  description?: string
}

export function SprintSelector({
  sprints,
  selectedSprintId,
  onChange,
  loading = false,
  description = 'Select a sprint to load live data from Jira.',
}: SprintSelectorProps) {
  const selectedSprint = sprints.find((sprint) => sprint.id === selectedSprintId) ?? null

  return (
    <section className="rounded-xl border border-gray-200 bg-white p-5 shadow-sm">
      <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div className="flex-1">
          <label htmlFor="sprint-selector" className="text-sm font-semibold text-gray-900">
            Sprint
          </label>
          <p className="mt-1 text-sm text-gray-600">{description}</p>
          <select
            id="sprint-selector"
            value={selectedSprintId ?? ''}
            onChange={(event) => onChange(Number(event.target.value))}
            disabled={loading || sprints.length === 0}
            className="mt-3 w-full max-w-xl rounded-md border border-gray-300 bg-white px-3 py-2 text-sm text-gray-900 shadow-sm focus:border-brand-500 focus:outline-none focus:ring-2 focus:ring-brand-500 disabled:cursor-not-allowed disabled:bg-gray-50"
          >
            <option value="" disabled>
              {loading ? 'Loading sprints...' : 'Select a sprint'}
            </option>
            {sprints.map((sprint) => (
              <option key={sprint.id} value={sprint.id}>
                {sprint.name} ({formatSprintState(sprint.state)})
              </option>
            ))}
          </select>
        </div>

        {selectedSprint && (
          <div className="grid gap-3 sm:grid-cols-3 lg:min-w-[420px]">
            <SprintMeta
              label="State"
              value={<SprintStateBadge state={selectedSprint.state} />}
            />
            <SprintMeta
              label="Start"
              icon={<CalendarDays className="h-4 w-4" aria-hidden="true" />}
              value={formatInstant(selectedSprint.startDate)}
            />
            <SprintMeta
              label="End"
              icon={<CalendarDays className="h-4 w-4" aria-hidden="true" />}
              value={formatInstant(selectedSprint.endDate)}
            />
          </div>
        )}
      </div>

      {selectedSprint?.goal && (
        <div className="mt-4 flex items-start gap-2 rounded-md bg-gray-50 px-3 py-2 text-sm text-gray-700">
          <Flag className="mt-0.5 h-4 w-4 shrink-0 text-gray-400" aria-hidden="true" />
          <p>{selectedSprint.goal}</p>
        </div>
      )}
    </section>
  )
}

function SprintMeta({
  label,
  value,
  icon,
}: {
  label: string
  value: ReactNode
  icon?: ReactNode
}) {
  return (
    <div className="rounded-md border border-gray-100 bg-gray-50 px-3 py-2">
      <p className="text-xs font-medium uppercase tracking-wide text-gray-500">{label}</p>
      <div className="mt-1 flex items-center gap-1.5 text-sm font-medium text-gray-900">
        {icon}
        {value}
      </div>
    </div>
  )
}

function SprintStateBadge({ state }: { state: string }) {
  const normalized = state.toLowerCase()
  const styles =
    normalized === 'active'
      ? 'bg-emerald-50 text-emerald-700 ring-emerald-200'
      : normalized === 'future'
        ? 'bg-brand-50 text-brand-600 ring-brand-200'
        : 'bg-gray-100 text-gray-700 ring-gray-200'

  return (
    <span className={`inline-flex rounded-full px-2 py-0.5 text-xs font-medium ring-1 ${styles}`}>
      {formatSprintState(state)}
    </span>
  )
}
