import { apiClient } from './apiClient'
import type { AnalyticsResponse } from './types'

export function getSprintAnalytics(
  podId: string,
  jiraSprintId: number,
): Promise<AnalyticsResponse> {
  return apiClient.get<AnalyticsResponse>(`/pods/${podId}/sprints/${jiraSprintId}/analytics`)
}
