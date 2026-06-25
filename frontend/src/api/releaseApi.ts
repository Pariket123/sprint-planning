import { apiClient } from './apiClient'
import type {
  CreateReleaseRequest,
  ReleaseResponse,
  UpdateReleaseCapacityRequest,
  UpdateReleaseRequest,
} from './types'

export function listReleases(podId: string): Promise<ReleaseResponse[]> {
  return apiClient.get<ReleaseResponse[]>(`/pods/${podId}/releases`)
}

export function getRelease(podId: string, releaseId: string): Promise<ReleaseResponse> {
  return apiClient.get<ReleaseResponse>(`/pods/${podId}/releases/${releaseId}`)
}

export function createRelease(
  podId: string,
  request: CreateReleaseRequest,
): Promise<ReleaseResponse> {
  return apiClient.post<ReleaseResponse>(`/pods/${podId}/releases`, request)
}

export function updateRelease(
  podId: string,
  releaseId: string,
  request: UpdateReleaseRequest,
): Promise<ReleaseResponse> {
  return apiClient.put<ReleaseResponse>(`/pods/${podId}/releases/${releaseId}`, request)
}

export function deactivateRelease(
  podId: string,
  releaseId: string,
): Promise<ReleaseResponse> {
  return apiClient.delete<ReleaseResponse>(`/pods/${podId}/releases/${releaseId}`)
}

export function updateReleaseCapacity(
  podId: string,
  releaseId: string,
  request: UpdateReleaseCapacityRequest,
): Promise<ReleaseResponse> {
  return apiClient.put<ReleaseResponse>(`/pods/${podId}/releases/${releaseId}/capacity`, request)
}
