import { Loader2 } from 'lucide-react'

interface LoadingStateProps {
  message?: string
  className?: string
}

export function LoadingState({
  message = 'Loading...',
  className = '',
}: LoadingStateProps) {
  return (
    <div
      className={`flex flex-col items-center justify-center gap-3 rounded-xl border border-gray-200 bg-white px-6 py-16 text-center shadow-sm ${className}`}
      role="status"
      aria-live="polite"
    >
      <Loader2 className="h-8 w-8 animate-spin text-brand-600" aria-hidden="true" />
      <p className="text-sm text-gray-600">{message}</p>
    </div>
  )
}

interface PageLoadingStateProps {
  message?: string
}

export function PageLoadingState({ message }: PageLoadingStateProps) {
  return (
    <div className="flex min-h-[320px] items-center justify-center">
      <LoadingState message={message} className="w-full max-w-md border-dashed" />
    </div>
  )
}
export function InlineLoadingState({ message = 'Loading...' }: { message?: string }) {
  return (
    <span className="inline-flex items-center gap-2 text-sm text-gray-500">
      <Loader2 className="h-4 w-4 animate-spin" aria-hidden="true" />
      {message}
    </span>
  )
}

