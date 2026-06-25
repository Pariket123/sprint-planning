import { apiClient } from './apiClient'
import type { TeamResponse } from './types'

export function listTeams(): Promise<TeamResponse[]> {
  return apiClient.get<TeamResponse[]>('/teams')
}
