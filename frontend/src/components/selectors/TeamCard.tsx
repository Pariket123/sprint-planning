import { Building2 } from 'lucide-react'
import { StatusBadge } from '../common'
import type { TeamResponse } from '../../api/types'

interface TeamCardProps {
  team: TeamResponse
  onSelect: (team: TeamResponse) => void
}

export function TeamCard({ team, onSelect }: TeamCardProps) {
  return (
    <button
      type="button"
      onClick={() => onSelect(team)}
      className="group flex w-full flex-col rounded-xl border border-gray-200 bg-white p-5 text-left shadow-sm transition hover:border-brand-300 hover:shadow-md focus:outline-none focus:ring-2 focus:ring-brand-500 focus:ring-offset-2"
    >
      <div className="flex items-start justify-between gap-3">
        <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-brand-50 text-brand-600">
          <Building2 className="h-5 w-5" aria-hidden="true" />
        </div>
        <StatusBadge active={team.active} />
      </div>

      <h3 className="mt-4 text-base font-semibold text-gray-900 group-hover:text-brand-600">
        {team.name}
      </h3>

      {team.code && (
        <p className="mt-1 text-sm text-gray-500">
          Code: <span className="font-medium text-gray-700">{team.code}</span>
        </p>
      )}

      <p className="mt-4 text-sm text-gray-600">Select this team to view pods and modules.</p>
    </button>
  )
}
