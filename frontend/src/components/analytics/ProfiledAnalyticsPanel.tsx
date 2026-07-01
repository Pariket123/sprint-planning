import type { AnalyticsResponse } from '../../api/types'
import {
  DEFAULT_DEV_SUB_DOMAIN_ANALYSIS_PROFILE_KEY,
  getAlternateDevSubDomainAnalysisProfile,
  getDevSubDomainAnalysisProfile,
} from '../../config/devSubDomainAnalysisProfiles'
import { AnalyticsInsightsPanel } from './AnalyticsInsightsPanel'

interface ProfiledAnalyticsPanelProps {
  analytics: AnalyticsResponse
  activeProfileKey?: string
  onProfileChange: (profileKey: string) => void
  switchingProfile?: boolean
}

export function ProfiledAnalyticsPanel({
  analytics,
  activeProfileKey = DEFAULT_DEV_SUB_DOMAIN_ANALYSIS_PROFILE_KEY,
  onProfileChange,
  switchingProfile = false,
}: ProfiledAnalyticsPanelProps) {
  const activeProfile = getDevSubDomainAnalysisProfile(activeProfileKey)
  const alternateProfile = getAlternateDevSubDomainAnalysisProfile(activeProfileKey)

  return (
    <div className="space-y-6">
      <p className="rounded-md border border-gray-200 bg-gray-50 px-4 py-3 text-sm text-gray-700">
        Showing analysis for <span className="font-medium text-gray-900">{activeProfile.label}</span>{' '}
        issues ({activeProfile.issueTypes.join(', ')}).
      </p>

      <AnalyticsInsightsPanel analytics={analytics} includeQaInDomainSummary />

      {alternateProfile && (
        <div className="flex justify-center border-t border-gray-200 pt-6">
          <button
            type="button"
            onClick={() => onProfileChange(alternateProfile.key)}
            disabled={switchingProfile}
            className="btn-secondary"
          >
            {switchingProfile
              ? 'Loading...'
              : `View ${alternateProfile.label.toLowerCase()} analysis`}
          </button>
        </div>
      )}
    </div>
  )
}
