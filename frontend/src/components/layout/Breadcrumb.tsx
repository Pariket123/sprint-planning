import { ChevronRight } from 'lucide-react'
import { Link } from 'react-router-dom'
import { useAppContext } from '../../context/AppContext'

export interface BreadcrumbItem {
  label: string
  to?: string
}

interface BreadcrumbProps {
  section?: string
  items?: BreadcrumbItem[]
}

export function Breadcrumb({ section, items }: BreadcrumbProps) {
  const {
    selectedTeamName,
    selectedPodName,
    selectedTeamId,
    selectedPodId,
    hasTeam,
    hasPod,
  } = useAppContext()

  const breadcrumbItems: BreadcrumbItem[] =
    items ??
    (() => {
      const built: BreadcrumbItem[] = []

      if (!hasTeam) {
        built.push({ label: 'Select Team' })
        return built
      }

      built.push({
        label: selectedTeamName ?? 'Team',
        to: `/teams/${selectedTeamId}/pods`,
      })

      if (!hasPod) {
        built.push({ label: 'Select Pod' })
        return built
      }

      built.push({
        label: selectedPodName ?? 'Pod',
        to: `/pods/${selectedPodId}`,
      })

      if (section) {
        built.push({ label: section })
      }

      return built
    })()

  return (
    <nav aria-label="Breadcrumb" className="flex items-center gap-1 text-sm">
      {breadcrumbItems.map((item, index) => {
        const isLast = index === breadcrumbItems.length - 1

        return (
          <div key={`${item.label}-${index}`} className="flex items-center gap-1">
            {index > 0 && (
              <ChevronRight className="h-4 w-4 shrink-0 text-gray-400" aria-hidden="true" />
            )}
            {item.to && !isLast ? (
              <Link
                to={item.to}
                className="truncate text-gray-500 transition hover:text-brand-600"
              >
                {item.label}
              </Link>
            ) : (
              <span
                className={`truncate ${isLast ? 'font-medium text-gray-900' : 'text-gray-500'}`}
                aria-current={isLast ? 'page' : undefined}
              >
                {item.label}
              </span>
            )}
          </div>
        )
      })}
    </nav>
  )
}
