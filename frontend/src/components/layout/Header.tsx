import { Breadcrumb, type BreadcrumbItem } from './Breadcrumb'
import { useAppContext } from '../../context/AppContext'

interface HeaderProps {
  section?: string
  breadcrumbItems?: BreadcrumbItem[]
}

export function Header({ section, breadcrumbItems }: HeaderProps) {
  const {
    selectedTeamName,
    selectedPodName,
    selectedSprintId,
    hasTeam,
    hasPod,
  } = useAppContext()

  return (
    <header className="border-b border-gray-200 bg-white px-6 py-4">
      <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
        <Breadcrumb section={section} items={breadcrumbItems} />

        {(hasTeam || hasPod || selectedSprintId !== null) && (
          <div className="flex flex-wrap items-center gap-2">
            {hasTeam && (
              <ContextChip label="Team" value={selectedTeamName ?? 'Selected'} />
            )}
            {hasPod && (
              <ContextChip label="Pod" value={selectedPodName ?? 'Selected'} />
            )}
            {selectedSprintId !== null && (
              <ContextChip label="Sprint" value={`#${selectedSprintId}`} />
            )}
          </div>
        )}
      </div>
    </header>
  )
}

function ContextChip({ label, value }: { label: string; value: string }) {
  return (
    <div className="inline-flex items-center gap-1.5 rounded-md border border-gray-200 bg-gray-50 px-2.5 py-1 text-xs">
      <span className="font-medium uppercase tracking-wide text-gray-500">{label}</span>
      <span className="font-medium text-gray-900">{value}</span>
    </div>
  )
}
