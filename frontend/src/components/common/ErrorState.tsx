import type { ReactNode } from 'react'
import { AlertCircle, RefreshCw } from 'lucide-react'

interface ErrorStateProps {
  title?: string
  message: string
  onRetry?: () => void
  action?: ReactNode
  className?: string
}

export function ErrorState({
  title = 'Something went wrong',
  message,
  onRetry,
  action,
  className = '',
}: ErrorStateProps) {
  return (
    <div
      className={`rounded-xl border border-red-200 bg-red-50 px-6 py-8 text-center shadow-sm ${className}`}
      role="alert"
    >
      <div className="mx-auto flex h-10 w-10 items-center justify-center rounded-full bg-red-100">
        <AlertCircle className="h-5 w-5 text-red-600" aria-hidden="true" />
      </div>
      <h3 className="mt-4 text-sm font-semibold text-red-900">{title}</h3>
      <p className="mt-2 text-sm text-red-700">{message}</p>
      {(onRetry || action) && (
        <div className="mt-5 flex items-center justify-center gap-3">
          {onRetry && (
            <button
              type="button"
              onClick={onRetry}
              className="inline-flex items-center gap-2 rounded-full border border-red-300 bg-white px-4 py-1.5 text-sm font-medium text-red-700 transition hover:bg-red-50"
            >
              <RefreshCw className="h-4 w-4" aria-hidden="true" />
              Try again
            </button>
          )}
          {action}
        </div>
      )}
    </div>
  )
}

interface PageErrorStateProps {
  message: string
  onRetry?: () => void
}

export function PageErrorState({ message, onRetry }: PageErrorStateProps) {
  return (
    <div className="flex min-h-[320px] items-center justify-center">
      <ErrorState message={message} onRetry={onRetry} className="w-full max-w-lg" />
    </div>
  )
}
