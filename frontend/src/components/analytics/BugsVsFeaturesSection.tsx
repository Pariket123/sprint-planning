import type { BugsVsFeaturesDto } from '../../api/types'
import { formatStoryPoints } from '../../utils/format'

interface BugsVsFeaturesSectionProps {
  data: BugsVsFeaturesDto
}

export function BugsVsFeaturesSection({ data }: BugsVsFeaturesSectionProps) {
  const categories = [
    { key: 'bugs', label: 'Bugs', color: 'border-red-200 bg-red-50', accent: 'text-red-700' },
    {
      key: 'features',
      label: 'Features',
      color: 'border-brand-200 bg-brand-50',
      accent: 'text-brand-600',
    },
    { key: 'other', label: 'Other', color: 'border-gray-200 bg-gray-50', accent: 'text-gray-700' },
  ] as const

  return (
    <div className="grid gap-4 md:grid-cols-3">
      {categories.map(({ key, label, color, accent }) => {
        const metrics = data[key]
        return (
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
        )
      })}
    </div>
  )
}
