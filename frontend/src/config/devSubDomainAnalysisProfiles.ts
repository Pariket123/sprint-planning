export interface DevSubDomainAnalysisProfile {
  key: string
  label: string
  issueTypes: string[]
}

/** Default release dev-sub-domain analysis profiles. Extend issueTypes to combine types (e.g. Story + Task). */
export const DEV_SUB_DOMAIN_ANALYSIS_PROFILES: DevSubDomainAnalysisProfile[] = [
  { key: 'story', label: 'Story', issueTypes: ['Story'] },
  { key: 'bug', label: 'Bug', issueTypes: ['Bug', 'Defect'] },
]

export const DEFAULT_DEV_SUB_DOMAIN_ANALYSIS_PROFILE_KEY = 'story'

export function getDevSubDomainAnalysisProfile(key: string): DevSubDomainAnalysisProfile {
  return (
    DEV_SUB_DOMAIN_ANALYSIS_PROFILES.find((profile) => profile.key === key) ??
    DEV_SUB_DOMAIN_ANALYSIS_PROFILES[0]
  )
}

export function getAlternateDevSubDomainAnalysisProfile(
  activeKey: string,
): DevSubDomainAnalysisProfile | null {
  return DEV_SUB_DOMAIN_ANALYSIS_PROFILES.find((profile) => profile.key !== activeKey) ?? null
}
