import { apiClient } from './apiClient'
import type { AnalyticsResponse } from './types'

export function getSprintAnalytics(
  podId: string,
  jiraSprintId: number,
  issueTypeProfile?: string | null,
): Promise<AnalyticsResponse> {
  const searchParams = new URLSearchParams()
  if (issueTypeProfile) {
    searchParams.set('issueTypeProfile', issueTypeProfile)
  }
  const query = searchParams.toString()
  const suffix = query ? `?${query}` : ''
  return apiClient.get<AnalyticsResponse>(
    `/pods/${podId}/sprints/${jiraSprintId}/analytics${suffix}`,
  )
}
