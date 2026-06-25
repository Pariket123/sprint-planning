import type { ReactNode } from 'react'
import { Inbox } from 'lucide-react'

interface EmptyStateProps {
  title: string
  description?: string
  icon?: ReactNode
  action?: ReactNode
  className?: string
}

export function EmptyState({
  title,
  description,
  icon,
  action,
  className = '',
}: EmptyStateProps) {
  return (
    <div
      className={`rounded-xl border border-dashed border-gray-300 bg-white px-6 py-12 text-center shadow-sm ${className}`}
    >
      <div className="mx-auto flex h-10 w-10 items-center justify-center rounded-full bg-gray-100 text-gray-500">
        {icon ?? <Inbox className="h-5 w-5" aria-hidden="true" />}
      </div>
      <h3 className="mt-4 text-sm font-semibold text-gray-900">{title}</h3>
      {description && <p className="mt-2 text-sm text-gray-600">{description}</p>}
      {action && <div className="mt-5 flex justify-center">{action}</div>}
    </div>
  )
}

interface PageEmptyStateProps {
  title: string
  description?: string
  action?: ReactNode
}

export function PageEmptyState({ title, description, action }: PageEmptyStateProps) {
  return (
    <div className="flex min-h-[320px] items-center justify-center">
      <EmptyState
        title={title}
        description={description}
        action={action}
        className="w-full max-w-lg"
      />
    </div>
  )
}
