import type { ComponentType, ReactNode } from 'react'
import { ChevronRight } from 'lucide-react'
import { Link } from 'react-router-dom'

interface ActionCardProps {
  title: string
  description: string
  to: string
  icon: ComponentType<{ className?: string }>
}

export function ActionCard({ title, description, to, icon: Icon }: ActionCardProps) {
  return (
    <Link
      to={to}
      className="group flex h-full flex-col rounded-xl border border-gray-200 bg-white p-6 shadow-sm transition hover:border-brand-300 hover:shadow-md focus:outline-none focus:ring-2 focus:ring-brand-500 focus:ring-offset-2"
    >
      <div className="flex items-start justify-between gap-4">
        <div className="flex h-12 w-12 items-center justify-center rounded-lg bg-brand-50 text-brand-600">
          <Icon className="h-6 w-6" aria-hidden="true" />
        </div>
        <ChevronRight
          className="h-5 w-5 text-gray-400 transition group-hover:translate-x-0.5 group-hover:text-brand-600"
          aria-hidden="true"
        />
      </div>

      <h3 className="mt-5 text-lg font-semibold text-gray-900 group-hover:text-brand-600">
        {title}
      </h3>
      <p className="mt-2 flex-1 text-sm leading-relaxed text-gray-600">{description}</p>

      <span className="mt-5 text-sm font-medium text-brand-600 group-hover:text-brand-600">
        Open workflow
      </span>
    </Link>
  )
}

interface SummaryItemProps {
  label: string
  value: ReactNode
}

export function SummaryItem({ label, value }: SummaryItemProps) {
  return (
    <div className="rounded-md border border-gray-100 bg-gray-50 px-4 py-3">
      <dt className="text-xs font-medium uppercase tracking-wide text-gray-500">{label}</dt>
      <dd className="mt-1 text-sm font-medium text-gray-900">{value}</dd>
    </div>
  )
}

function formatList(values: string[] | null | undefined): string {
  if (!values || values.length === 0) {
    return '-'
  }
  return values.join(', ')
}

function formatDomainValues(values: Record<string, string> | null | undefined): string {
  if (!values || Object.keys(values).length === 0) {
    return '-'
  }
  return Object.entries(values)
    .map(([key, label]) => `${key} (${label})`)
    .join(', ')
}

export { formatList, formatDomainValues }
