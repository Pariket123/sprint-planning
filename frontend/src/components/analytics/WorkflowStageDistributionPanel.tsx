import type { WorkflowStageDistributionDto } from '../../api/types'

interface WorkflowStageDistributionPanelProps {
  distribution: WorkflowStageDistributionDto
}

export function WorkflowStageDistributionPanel({
  distribution,
}: WorkflowStageDistributionPanelProps) {
  return (
    <div className="overflow-x-auto">
      <table className="min-w-full divide-y divide-gray-200 text-sm">
        <thead className="bg-gray-50">
          <tr>
            <th className="px-4 py-3 text-left font-medium text-gray-600">Stage</th>
            <th className="px-4 py-3 text-right font-medium text-gray-600">Issues</th>
            <th className="px-4 py-3 text-right font-medium text-gray-600">Share</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-100 bg-white">
          {distribution.sections.map((section) => (
            <tr key={section.key}>
              <td className="px-4 py-3 text-gray-900">{section.label}</td>
              <td className="px-4 py-3 text-right text-gray-700">
                {section.count}/{section.totalIssues}
              </td>
              <td className="px-4 py-3 text-right text-gray-700">{section.ratio.toFixed(1)}%</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
