import type { BugsVsFeaturesDto } from '../../api/types'
import { formatStoryPoints } from '../../utils/format'

interface BugsVsFeaturesSectionProps {
  data: BugsVsFeaturesDto
}

export function BugsVsFeaturesSection({ data }: BugsVsFeaturesSectionProps) {
  const primaryCategories = [
    { key: 'bugs', label: 'Bugs', metrics: data.bugs, color: 'border-red-200 bg-red-50', accent: 'text-red-700' },
    {
      key: 'stories',
      label: 'Story',
      metrics: data.stories,
      color: 'border-brand-200 bg-brand-50',
      accent: 'text-brand-600',
    },
  ] as const

  return (
    <div className="space-y-4">
      <div className="grid gap-4 md:grid-cols-2">
        {primaryCategories.map(({ key, label, metrics, color, accent }) => (
          <div key={key} className={`rounded-xl border p-4 ${color}`}>
            <p className={`text-sm font-semibold ${accent}`}>{label}</p>
            <dl className="mt-3 space-y-2 text-sm">
              <div className="flex justify-between gap-4">
                <dt className="text-gray-600">Issues</dt>
                <dd className="font-medium text-gray-900">{metrics.count}</dd>
              </div>
              <div className="flex justify-between gap-4">
                <dt className="text-gray-600">Story points</dt>
                <dd className="font-medium text-gray-900">
                  {formatStoryPoints(metrics.storyPoints)}
                </dd>
              </div>
            </dl>
          </div>
        ))}
      </div>

      {data.otherTypes.length > 0 && (
        <div className="rounded-xl border border-gray-200 bg-gray-50 p-4">
          <p className="text-sm font-semibold text-gray-700">Other issue types</p>
          <div className="mt-3 divide-y divide-gray-200">
            {data.otherTypes.map((item) => (
              <dl
                key={item.issueType}
                className="grid gap-2 py-3 text-sm sm:grid-cols-[minmax(0,1fr)_auto_auto] sm:items-center sm:gap-6"
              >
                <dt className="font-medium text-gray-900">{item.issueType}</dt>
                <div className="flex justify-between gap-4 sm:justify-end">
                  <span className="text-gray-600">Issues</span>
                  <span className="font-medium text-gray-900">{item.count}</span>
                </div>
                <div className="flex justify-between gap-4 sm:justify-end">
                  <span className="text-gray-600">Story points</span>
                  <span className="font-medium text-gray-900">
                    {formatStoryPoints(item.storyPoints)}
                  </span>
                </div>
              </dl>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}
