import { apiClient } from './apiClient'
import type {
  AnalyticsResponse,
  IssueSearchFilters,
  IssueSearchPageDto,
  IssueSearchQueryParams,
  IssueSearchReleaseRequest,
  ReleaseCapacitySummaryDto,
} from './types'

function buildSearchQuery(params?: IssueSearchQueryParams): string {
  const searchParams = new URLSearchParams()
  searchParams.set('startAt', String(params?.startAt ?? 0))
  searchParams.set('maxResults', String(params?.maxResults ?? 50))
  return searchParams.toString()
}

export function searchIssuesInPod(
  podId: string,
  filters?: IssueSearchFilters | null,
  pagination?: IssueSearchQueryParams,
): Promise<IssueSearchPageDto> {
  return apiClient.post<IssueSearchPageDto>(
    `/pods/${podId}/issues/search?${buildSearchQuery(pagination)}`,
    filters ?? {},
  )
}

export function searchIssuesInRelease(
  podId: string,
  releaseId: string,
  request?: IssueSearchReleaseRequest | null,
  pagination?: IssueSearchQueryParams,
): Promise<IssueSearchPageDto> {
  return apiClient.post<IssueSearchPageDto>(
    `/pods/${podId}/releases/${releaseId}/issues/search?${buildSearchQuery(pagination)}`,
    request ?? {},
  )
}

export function getReleaseIssuesAnalytics(
  podId: string,
  releaseId: string,
  request?: IssueSearchReleaseRequest | null,
): Promise<AnalyticsResponse> {
  return apiClient.post<AnalyticsResponse>(
    `/pods/${podId}/releases/${releaseId}/issues/analytics`,
    request ?? {},
  )
}

export function getReleaseCapacityMetrics(
  podId: string,
  releaseId: string,
  request?: IssueSearchReleaseRequest | null,
): Promise<ReleaseCapacitySummaryDto> {
  return apiClient.post<ReleaseCapacitySummaryDto>(
    `/pods/${podId}/releases/${releaseId}/capacity/metrics`,
    request ?? {},
  )
}
