import { apiClient } from './apiClient'
import type { SprintView } from './types'

export function listSprints(
  podId: string,
  state = 'active,future,closed',
): Promise<SprintView[]> {
  const params = new URLSearchParams({ state })
  return apiClient.get<SprintView[]>(`/pods/${podId}/sprints?${params.toString()}`)
}

export function listActiveAndFutureSprints(podId: string): Promise<SprintView[]> {
  return listSprints(podId, 'active,future')
}
