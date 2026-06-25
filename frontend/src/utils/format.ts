import type { Domain, DomainBreakdownItemDto } from '../api/types'

const DOMAIN_LABELS: Record<Domain, string> = {
  BE: 'Backend',
  UI: 'Ui',
  AI: 'Ai',
  DEV: 'Dev',
  QA: 'QA',
  DESIGN: 'Design',
  PRODUCT: 'Product',
  UNKNOWN: 'Unmapped',
}

const DOMAIN_DISPLAY_ORDER: Domain[] = ['BE', 'UI', 'AI', 'DEV', 'QA', 'DESIGN', 'PRODUCT', 'UNKNOWN']

export function formatDomain(domain: Domain | string | null | undefined): string {
  if (!domain || domain === 'UNKNOWN') {
    return 'Unmapped'
  }
  if (domain in DOMAIN_LABELS) {
    return DOMAIN_LABELS[domain as Domain]
  }
  return domain
}

export function sortDomainBreakdown(items: DomainBreakdownItemDto[]): DomainBreakdownItemDto[] {
  const order = new Map(DOMAIN_DISPLAY_ORDER.map((domain, index) => [domain, index]))
  return [...items].sort(
    (left, right) =>
      (order.get(left.domain) ?? Number.MAX_SAFE_INTEGER) -
      (order.get(right.domain) ?? Number.MAX_SAFE_INTEGER),
  )
}

export function formatStoryPoints(value: number | null | undefined): string {
  if (value === null || value === undefined) {
    return '-'
  }
  return Number.isInteger(value) ? String(value) : value.toFixed(1)
}

export function formatPercent(value: number | null | undefined): string {
  if (value === null || value === undefined) {
    return '-'
  }
  return `${value.toFixed(1)}%`
}

export function formatCount(value: number | null | undefined): string {
  if (value === null || value === undefined) {
    return '-'
  }
  return String(value)
}

export function formatInstant(value: string | null | undefined): string {
  if (!value) {
    return '-'
  }

  try {
    return new Intl.DateTimeFormat(undefined, {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    }).format(new Date(value))
  } catch {
    return value
  }
}

export function formatSprintState(state: string | null | undefined): string {
  if (!state) {
    return 'Unknown'
  }
  return state.charAt(0).toUpperCase() + state.slice(1).toLowerCase()
}
