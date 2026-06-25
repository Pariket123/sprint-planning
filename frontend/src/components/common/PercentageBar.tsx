interface PercentageBarProps {
  label: string
  value: number
  displayValue?: string
  colorClass?: string
}

export function PercentageBar({
  label,
  value,
  displayValue,
  colorClass = 'bg-brand-600',
}: PercentageBarProps) {
  const clamped = Math.max(0, Math.min(100, value))

  return (
    <div>
      <div className="mb-1 flex items-center justify-between gap-3 text-xs">
        <span className="font-medium text-gray-600">{label}</span>
        <span className="font-medium text-gray-900">{displayValue ?? `${clamped.toFixed(1)}%`}</span>
      </div>
      <div className="h-2 overflow-hidden rounded-full bg-gray-100">
        <div
          className={`h-full rounded-full transition-all ${colorClass}`}
          style={{ width: `${clamped}%` }}
          role="progressbar"
          aria-valuenow={clamped}
          aria-valuemin={0}
          aria-valuemax={100}
          aria-label={label}
        />
      </div>
    </div>
  )
}
