import type { BugsVsFeaturesDto } from '../../api/types'
import { formatStoryPoints } from '../../utils/format'

interface BugsVsFeaturesSectionProps {
  data: BugsVsFeaturesDto
}

function MetricsCard({
  label,
  count,
  storyPoints,
  color,
  accent,
}: {
  label: string
  count: number
  storyPoints: number
  color: string
  accent: string
}) {
  return (
    <div className={`rounded-xl border p-4 ${color}`}>
      <p className={`text-sm font-semibold ${accent}`}>{label}</p>
      <dl className="mt-3 space-y-2 text-sm">
        <div className="flex justify-between gap-4">
          <dt className="text-gray-600">Issues</dt>
          <dd className="font-medium text-gray-900">{count}</dd>
        </div>
        <div className="flex justify-between gap-4">
          <dt className="text-gray-600">Story points</dt>
          <dd className="font-medium text-gray-900">{formatStoryPoints(storyPoints)}</dd>
        </div>
      </dl>
    </div>
  )
}

export function BugsVsFeaturesSection({ data }: BugsVsFeaturesSectionProps) {
  const columnCount = data.otherTypes.length > 0 ? 3 : 2

  return (
    <div
      className={`grid gap-4 ${
        columnCount === 3 ? 'md:grid-cols-3' : 'md:grid-cols-2'
      }`}
    >
      <MetricsCard
        label="Bugs"
        count={data.bugs.count}
        storyPoints={data.bugs.storyPoints}
        color="border-red-200 bg-red-50"
        accent="text-red-700"
      />
      <MetricsCard
        label="Story"
        count={data.stories.count}
        storyPoints={data.stories.storyPoints}
        color="border-brand-200 bg-brand-50"
        accent="text-brand-600"
      />
      {data.otherTypes.length > 0 && (
        <div className="rounded-xl border border-gray-200 bg-gray-50 p-4">
          <p className="text-sm font-semibold text-gray-700">Other issue types</p>
          <div className="mt-3 space-y-4">
            {data.otherTypes.map((item) => (
              <div key={item.issueType}>
                <p className="text-sm font-medium text-gray-900">{item.issueType}</p>
                <dl className="mt-2 space-y-2 text-sm">
                  <div className="flex justify-between gap-4">
                    <dt className="text-gray-600">Issues</dt>
                    <dd className="font-medium text-gray-900">{item.count}</dd>
                  </div>
                  <div className="flex justify-between gap-4">
                    <dt className="text-gray-600">Story points</dt>
                    <dd className="font-medium text-gray-900">
                      {formatStoryPoints(item.storyPoints)}
                    </dd>
                  </div>
                </dl>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}
