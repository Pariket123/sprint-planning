import { apiClient } from './apiClient'
import type {
  BacklogPageDto,
  IssueMoveRequest,
  PlannedIssueViewDto,
  PlannedScopeDto,
  PlanningSummaryDto,
  PlanningValidationResultDto,
  PlanningViewDto,
  RecordRolloverRequest,
  RolloverIssueDto,
  UpdateCapacityRequest,
  UpdateLeavesRequest,
  UpdateOverridesRequest,
  UpdatePlannedScopeRequest,
} from './types'

function planningPath(podId: string, jiraSprintId: number): string {
  return `/pods/${podId}/sprints/${jiraSprintId}/planning`
}

export function getPlanning(podId: string, jiraSprintId: number): Promise<PlanningViewDto> {
  return apiClient.get<PlanningViewDto>(planningPath(podId, jiraSprintId))
}

export function getPlanningSummary(
  podId: string,
  jiraSprintId: number,
): Promise<PlanningSummaryDto> {
  return apiClient.get<PlanningSummaryDto>(`${planningPath(podId, jiraSprintId)}/summary`)
}

export function updateCapacity(
  podId: string,
  jiraSprintId: number,
  request: UpdateCapacityRequest,
): Promise<void> {
  return apiClient.put<void>(`${planningPath(podId, jiraSprintId)}/capacity`, request)
}

export function updateLeaves(
  podId: string,
  jiraSprintId: number,
  request: UpdateLeavesRequest,
): Promise<void> {
  return apiClient.put<void>(`${planningPath(podId, jiraSprintId)}/leaves`, request)
}

export function updateOverrides(
  podId: string,
  jiraSprintId: number,
  request: UpdateOverridesRequest,
): Promise<void> {
  return apiClient.put<void>(`${planningPath(podId, jiraSprintId)}/overrides`, request)
}

export function validatePlanning(
  podId: string,
  jiraSprintId: number,
): Promise<PlanningValidationResultDto> {
  return apiClient.post<PlanningValidationResultDto>(
    `${planningPath(podId, jiraSprintId)}/validate`,
  )
}

export function getPlannedScope(podId: string, jiraSprintId: number): Promise<PlannedScopeDto> {
  return apiClient.get<PlannedScopeDto>(`${planningPath(podId, jiraSprintId)}/planned-scope`)
}

export function updatePlannedScope(
  podId: string,
  jiraSprintId: number,
  request: UpdatePlannedScopeRequest,
): Promise<PlannedScopeDto> {
  return apiClient.put<PlannedScopeDto>(
    `${planningPath(podId, jiraSprintId)}/planned-scope`,
    request,
  )
}

export function getPlannedIssues(
  podId: string,
  jiraSprintId: number,
): Promise<PlannedIssueViewDto[]> {
  return apiClient.get<PlannedIssueViewDto[]>(
    `${planningPath(podId, jiraSprintId)}/planned-issues`,
  )
}

export function getBacklog(
  podId: string,
  startAt = 0,
  maxResults = 50,
): Promise<BacklogPageDto> {
  const params = new URLSearchParams({
    startAt: String(startAt),
    maxResults: String(maxResults),
  })
  return apiClient.get<BacklogPageDto>(`/pods/${podId}/backlog?${params.toString()}`)
}

export function moveIssuesToBacklog(
  podId: string,
  request: IssueMoveRequest,
  startAt = 0,
  maxResults = 50,
): Promise<BacklogPageDto> {
  const params = new URLSearchParams({
    startAt: String(startAt),
    maxResults: String(maxResults),
  })
  return apiClient.post<BacklogPageDto>(
    `/pods/${podId}/backlog/move?${params.toString()}`,
    request,
  )
}

export function moveIssuesToSprint(
  podId: string,
  jiraSprintId: number,
  request: IssueMoveRequest,
): Promise<PlanningViewDto> {
  return apiClient.post<PlanningViewDto>(
    `/pods/${podId}/sprints/${jiraSprintId}/issues/move`,
    request,
  )
}

export function recordRollover(
  podId: string,
  fromSprintId: number,
  request: RecordRolloverRequest,
): Promise<RolloverIssueDto> {
  return apiClient.post<RolloverIssueDto>(
    `/pods/${podId}/sprints/${fromSprintId}/rollover`,
    request,
  )
}

export function getRolloverRecords(
  podId: string,
  jiraSprintId: number,
): Promise<RolloverIssueDto[]> {
  return apiClient.get<RolloverIssueDto[]>(`/pods/${podId}/sprints/${jiraSprintId}/rollover`)
}

export function getOutgoingRollovers(
  podId: string,
  jiraSprintId: number,
): Promise<RolloverIssueDto[]> {
  return apiClient.get<RolloverIssueDto[]>(
    `/pods/${podId}/sprints/${jiraSprintId}/rollover/outgoing`,
  )
}

export function getIncomingRollovers(
  podId: string,
  jiraSprintId: number,
): Promise<RolloverIssueDto[]> {
  return apiClient.get<RolloverIssueDto[]>(
    `/pods/${podId}/sprints/${jiraSprintId}/rollover/incoming`,
  )
}
