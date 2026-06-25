import type { ReactNode } from 'react'

interface AnalyticsSummaryCardProps {
  label: string
  value: ReactNode
  hint?: string
}

export function AnalyticsSummaryCard({ label, value, hint }: AnalyticsSummaryCardProps) {
  return (
    <div className="rounded-xl border border-gray-200 bg-white p-4 shadow-sm">
      <p className="text-xs font-medium uppercase tracking-wide text-gray-500">{label}</p>
      <p className="mt-2 text-2xl font-semibold text-gray-900">{value}</p>
      {hint && <p className="mt-1 text-xs text-gray-500">{hint}</p>}
    </div>
  )
}
