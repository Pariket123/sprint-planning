import { apiClient } from './apiClient'
import type { PodResponse } from './types'

export function listPods(teamId: string): Promise<PodResponse[]> {
  return apiClient.get<PodResponse[]>(`/teams/${teamId}/pods`)
}

export function getPod(podId: string): Promise<PodResponse> {
  return apiClient.get<PodResponse>(`/pods/${podId}`)
}
