import type { StatusCategory } from '../../api/types'

const CATEGORY_STYLES: Record<StatusCategory, string> = {
  TODO: 'bg-gray-100 text-gray-700 ring-gray-200',
  IN_PROGRESS: 'bg-brand-50 text-brand-600 ring-brand-200',
  DONE: 'bg-emerald-50 text-emerald-700 ring-emerald-200',
  UNKNOWN: 'bg-amber-50 text-amber-700 ring-amber-200',
}

const CATEGORY_LABELS: Record<StatusCategory, string> = {
  TODO: 'To Do',
  IN_PROGRESS: 'In Progress',
  DONE: 'Done',
  UNKNOWN: 'Unknown',
}

interface StatusCategoryBadgeProps {
  category: StatusCategory
}

export function StatusCategoryBadge({ category }: StatusCategoryBadgeProps) {
  return (
    <span
      className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ring-1 ${CATEGORY_STYLES[category]}`}
    >
      {CATEGORY_LABELS[category]}
    </span>
  )
}
