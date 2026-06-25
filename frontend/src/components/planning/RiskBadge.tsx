import type { CapacityRiskStatus, RiskLevel } from '../../api/types'

const CAPACITY_RISK_STYLES: Record<CapacityRiskStatus, string> = {
  OK: 'bg-emerald-50 text-emerald-700 ring-emerald-200',
  NEAR_CAPACITY: 'bg-amber-50 text-amber-700 ring-amber-200',
  OVER_CAPACITY: 'bg-red-50 text-red-700 ring-red-200',
}

const CAPACITY_RISK_LABELS: Record<CapacityRiskStatus, string> = {
  OK: 'OK',
  NEAR_CAPACITY: 'Near capacity',
  OVER_CAPACITY: 'Over capacity',
}

const RISK_LEVEL_STYLES: Record<RiskLevel, string> = {
  LOW: 'bg-emerald-50 text-emerald-700 ring-emerald-200',
  MEDIUM: 'bg-amber-50 text-amber-700 ring-amber-200',
  HIGH: 'bg-red-50 text-red-700 ring-red-200',
}

interface RiskBadgeProps {
  risk: CapacityRiskStatus | RiskLevel | null | undefined
}

export function RiskBadge({ risk }: RiskBadgeProps) {
  if (!risk) {
    return (
      <span className="inline-flex rounded-full bg-brand-50 px-2 py-0.5 text-xs font-medium text-brand-600 ring-1 ring-brand-200">
        Unknown
      </span>
    )
  }

  const styles =
    risk in CAPACITY_RISK_STYLES
      ? CAPACITY_RISK_STYLES[risk as CapacityRiskStatus]
      : RISK_LEVEL_STYLES[risk as RiskLevel]

  const label =
    risk in CAPACITY_RISK_LABELS
      ? CAPACITY_RISK_LABELS[risk as CapacityRiskStatus]
      : risk

  return (
    <span className={`inline-flex rounded-full px-2 py-0.5 text-xs font-medium ring-1 ${styles}`}>
      {label}
    </span>
  )
}
