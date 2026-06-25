interface StatusBadgeProps {
  active: boolean
  activeLabel?: string
  inactiveLabel?: string
}

export function StatusBadge({
  active,
  activeLabel = 'Active',
  inactiveLabel = 'Inactive',
}: StatusBadgeProps) {
  return (
    <span
      className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${
        active
          ? 'bg-emerald-50 text-emerald-700 ring-1 ring-emerald-200'
          : 'bg-gray-100 text-gray-600 ring-1 ring-gray-200'
      }`}
    >
      {active ? activeLabel : inactiveLabel}
    </span>
  )
}
