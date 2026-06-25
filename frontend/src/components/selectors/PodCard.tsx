import { Boxes } from 'lucide-react'
import { StatusBadge } from '../common'
import type { PodResponse } from '../../api/types'

function formatList(values: string[] | null | undefined): string {
  if (!values || values.length === 0) {
    return '-'
  }
  return values.join(', ')
}

interface PodCardProps {
  pod: PodResponse
  onSelect: (pod: PodResponse) => void
}

export function PodCard({ pod, onSelect }: PodCardProps) {
  const jiraConfig = pod.jiraConfig

  return (
    <button
      type="button"
      onClick={() => onSelect(pod)}
      className="group flex w-full flex-col rounded-xl border border-gray-200 bg-white p-5 text-left shadow-sm transition hover:border-brand-300 hover:shadow-md focus:outline-none focus:ring-2 focus:ring-brand-500 focus:ring-offset-2"
    >
      <div className="flex items-start justify-between gap-3">
        <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-brand-50 text-brand-600">
          <Boxes className="h-5 w-5" aria-hidden="true" />
        </div>
        <StatusBadge active={pod.active} />
      </div>

      <h3 className="mt-4 text-base font-semibold text-gray-900 group-hover:text-brand-600">
        {pod.name}
      </h3>

      {pod.code && (
        <p className="mt-1 text-sm text-gray-500">
          Code: <span className="font-medium text-gray-700">{pod.code}</span>
        </p>
      )}

      <dl className="mt-4 space-y-2 border-t border-gray-100 pt-4 text-sm">
        <div className="flex justify-between gap-4">
          <dt className="text-gray-500">Jira projects</dt>
          <dd className="text-right font-medium text-gray-900">
            {formatList(jiraConfig?.projectKeys)}
          </dd>
        </div>
        <div className="flex justify-between gap-4">
          <dt className="text-gray-500">Board ID</dt>
          <dd className="text-right font-medium text-gray-900">
            {jiraConfig?.boardId ?? '-'}
          </dd>
        </div>
      </dl>
    </button>
  )
}
