export interface ErrorDetail {
  code: string
  message: string
}

export interface ApiResponse<T> {
  success: boolean
  data: T | null
  error: ErrorDetail | null
}

export class ApiError extends Error {
  readonly code: string
  readonly status: number

  constructor(message: string, code = 'API_ERROR', status = 0) {
    super(message)
    this.name = 'ApiError'
    this.code = code
    this.status = status
  }
}

export interface TeamResponse {
  id: string
  code: string
  name: string
  active: boolean
}

export interface PodJiraConfigSummary {
  boardId: number | null
  projectKeys: string[] | null
  storyPointsField: string | null
  domainField: string | null
  domainValues: Record<string, string> | null
}

export interface PodResponse {
  id: string
  teamId: string
  code: string
  name: string
  active: boolean
  jiraConfig: PodJiraConfigSummary | null
}

export interface SprintView {
  id: number
  name: string
  state: string
  startDate: string | null
  endDate: string | null
  goal: string | null
}

export interface ReleaseResponse {
  id: string
  teamId: string
  name: string
  description: string | null
  baseJql: string | null
  durationDays: number | null
  startDate: string | null
  capacity: PersonCapacity[] | null
  leavePercent: number | null
  capacityAllocation: CapacityAllocationPercents[] | null
  active: boolean
  createdAt: string | null
  updatedAt: string | null
}

export interface CreateReleaseRequest {
  name: string
  description?: string | null
  baseJql?: string | null
  durationDays?: number | null
  startDate?: string | null
}

export interface UpdateReleaseRequest {
  name: string
  description?: string | null
  baseJql?: string | null
  durationDays?: number | null
  startDate?: string | null
}

export interface IssueSearchReleaseRequest {
  additionalJql?: string | null
  issueTypeProfile?: string | null
}

export interface ReleaseCapacitySummaryDto {
  releaseId: string
  durationDays: number | null
  totalAvailableCapacity: number
  totalCommittedStoryPoints: number
  totalIssueCount: number
  domainMetrics: DomainPlanningMetricsDto[]
  riskLevel: RiskLevel
  capacityAllocationTable: CapacityAllocationTableDto | null
}

export interface UpdateReleaseCapacityRequest {
  capacity: PersonCapacity[]
  leavePercent?: number | null
  capacityAllocation?: CapacityAllocationPercents[] | null
}

export interface IssueSearchQueryParams {
  startAt?: number
  maxResults?: number
}

export interface IssueSearchFilters {
  issueTypes?: string[] | null
  statuses?: string[] | null
  domains?: string[] | null
  sprintIds?: number[] | null
  fixVersions?: string[] | null
  fixVersionExcludes?: string[] | null
  assigneeIds?: string[] | null
  priorities?: string[] | null
  issueKeys?: string[] | null
  podIds?: string[] | null
  labels?: string[] | null
  components?: string[] | null
}

export interface DomainAllocation {
  domain: Domain
  storyPoints: number
  completed: boolean
}

export interface TicketViewDto {
  key: string
  summary: string
  issueType: string
  status: string
  statusCategory: StatusCategory
  storyPoints: number | null
  domain: Domain
  domainLabel?: string | null
  domainAllocations?: DomainAllocation[] | null
  assigneeId: string | null
  assigneeDisplayName: string | null
  priority: string | null
  fixVersions: string[] | null
  sprintIds: number[] | null
  labels: string[] | null
  components: string[] | null
}

export interface IssueSearchPageDto {
  issues: TicketViewDto[]
  startAt: number
  maxResults: number
  total: number
  last: boolean
}

export type Domain =
  | 'DEV'
  | 'QA'
  | 'DESIGN'
  | 'BE'
  | 'UI'
  | 'AI'
  | 'PRODUCT'
  | 'UNKNOWN'

export type StatusCategory = 'TODO' | 'IN_PROGRESS' | 'DONE' | 'UNKNOWN'

export interface CategoryMetricsDto {
  count: number
  storyPoints: number
}

export interface IssueCountsDto {
  total: number
  completed: number
  remaining: number
}

export interface IssueTypeMetricsDto {
  issueType: string
  count: number
  storyPoints: number
}

export interface BugsVsFeaturesDto {
  bugs: CategoryMetricsDto
  stories: CategoryMetricsDto
  otherTypes: IssueTypeMetricsDto[]
}

export interface StatusDistributionItemDto {
  status: string
  statusCategory: StatusCategory
  count: number
  storyPoints: number
}

export interface DomainBreakdownItemDto {
  domain: Domain
  count: number
  storyPoints: number
  issueCountPercentage: number
  storyPointPercentage: number
  completedIssueCount: number
  completedStoryPoints: number
  remainingIssueCount: number
  remainingStoryPoints: number
  issueCompletionPercentage: number
  storyPointCompletionPercentage: number
}

export interface WorkflowStageSectionItemDto {
  key: string
  label: string
  count: number
  totalIssues: number
  ratio: number
}

export interface WorkflowStageDistributionDto {
  totalIssues: number
  sections: WorkflowStageSectionItemDto[]
}

export interface DevSubDomainItemDto {
  domain: Domain
  applicableIssueCount: number
  completedIssueCount: number
  issueCompletionRatio: number
  totalStoryPoints: number
  completedStoryPoints: number
  storyPointCompletionRatio: number
}

export interface DevSubDomainMetricsDto {
  subDomainPoolIssueCount: number
  subDomains: DevSubDomainItemDto[]
}

export interface AnalyticsResponse {
  jiraSprintId: number
  sprintName: string
  totalStoryPoints: number
  completedStoryPoints: number
  remainingStoryPoints: number
  issueCounts: IssueCountsDto
  bugsVsFeatures: BugsVsFeaturesDto
  statusDistribution: StatusDistributionItemDto[]
  domainBreakdown: DomainBreakdownItemDto[]
  workflowStageDistribution: WorkflowStageDistributionDto | null
  devSubDomainMetrics: DevSubDomainMetricsDto | null
}

export type CapacityRiskStatus = 'OK' | 'NEAR_CAPACITY' | 'OVER_CAPACITY'
export type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH'
export type LeaveType = 'LEAVE' | 'HOLIDAY'
export type OverrideAction = 'INCLUDE' | 'EXCLUDE'
export type PlanningWarningCode = 'OVER_CAPACITY' | 'DOMAIN_IMBALANCE' | 'HIGH_UTILIZATION'

export interface IssueView {
  key: string
  summary: string
  domain: Domain
  domainLabel?: string | null
  storyPoints: number | null
  issueType: string
  status: string
  statusCategory: StatusCategory
  domainAllocations?: DomainAllocation[] | null
}

export interface PersonCapacity {
  personName: string
  domain: Domain
  bandwidthPercent: number
  velocity?: number
}

export interface CapacityAllocationPercents {
  key: string
  roadmapPercent: number
  bugSupportPercent: number
}

export interface CapacityAllocationRowDto {
  key: string
  label: string
  availableStoryPoints: number
  roadmapPercent: number
  bugSupportPercent: number
  plannedRoadmapStoryPoints: number
  plannedBugSupportStoryPoints: number
}

export interface CapacityAllocationTableDto {
  rows: CapacityAllocationRowDto[]
}

export interface LeaveEntry {
  personName: string
  startDate: string
  endDate: string
  domain: Domain
  type: LeaveType
}

export interface PlanningOverride {
  issueKey: string
  action: OverrideAction
  notes: string | null
}

export interface RolloverIssueDto {
  issueKey: string
  fromSprintId: number
  toSprintId: number
  statusAtRollover: string
  storyPointsAtRollover: number | null
  domain: Domain
  domainLabel?: string | null
  rolledOverAt: string
  rolledOverBy: string
  notes: string | null
}

export interface DomainPlanningMetricsDto {
  domain: Domain
  availableCapacity: number
  rollover: number
  selectedStoryPoints: number
  selectedIssueCount: number
  suggestedCommitment: number
  committedStoryPoints: number
  utilizationPercent: number
  capacityRisk: CapacityRiskStatus
}

export interface PlanningViewDto {
  jiraSprintId: number
  sprint: SprintView
  capacity: PersonCapacity[]
  leaves: LeaveEntry[]
  overrides: PlanningOverride[]
  capacityAllocation: CapacityAllocationPercents[]
  rolloverStoryPoints: Record<string, number>
  resolvedRollover: Record<string, number>
  sprintIssueCount: number
  selectedIssueCount: number
  selectedIssueKeys: string[]
  plannedIssueKeys: string[]
  committedIssueKeys: string[]
  rolloverIssues: RolloverIssueDto[]
  domainMetrics: DomainPlanningMetricsDto[]
  capacityAllocationTable: CapacityAllocationTableDto | null
}

export interface PlanningIssuesPageDto {
  sprintIssues: IssueView[]
  selectedIssues: IssueView[]
  startAt: number
  maxResults: number
  sprintIssueTotal: number
  last: boolean
}

export interface PlanningSummaryDto {
  jiraSprintId: number
  totalAvailableCapacity: number
  totalRollover: number
  totalSelectedStoryPoints: number
  totalSelectedIssueCount: number
  domainMetrics: DomainPlanningMetricsDto[]
  riskLevel: RiskLevel
  capacityAllocationTable: CapacityAllocationTableDto | null
}

export interface PlanningDataDto {
  id: string
  podId: string
  jiraSprintId: number
  capacity: PersonCapacity[]
  leaves: LeaveEntry[]
  overrides: PlanningOverride[]
  rolloverStoryPoints: Record<string, number>
  updatedAt: string | null
}

export interface UpdateCapacityAllocationRequest {
  capacityAllocation: CapacityAllocationPercents[]
}

export interface PlanningWarningDto {
  code: PlanningWarningCode
  message: string
  domain: Domain | null
}

export interface PlanningValidationResultDto {
  warnings: PlanningWarningDto[]
  riskLevel: RiskLevel
}

export interface UpdateCapacityRequest {
  capacity: PersonCapacity[]
}

export interface UpdateLeavesRequest {
  leaves: LeaveEntry[]
}

export interface UpdateOverridesRequest {
  overrides: PlanningOverride[]
}

export interface PlannedScopeDto {
  plannedIssueKeys: string[]
  plannedScopeCapturedAt: string | null
}

export interface PlannedIssueViewDto {
  key: string
  summary: string
  issueType: string
  status: string
  statusCategory: StatusCategory
  storyPoints: number | null
  domain: Domain
  domainLabel?: string | null
  plannedSprintId: number
  currentSprintId: number | null
  rolledOver: boolean
}

export interface BacklogPageDto {
  issues: IssueView[]
  startAt: number
  maxResults: number
  total: number
  last: boolean
}

export interface IssueMoveRequest {
  issueKeys: string[]
  addToPlannedScope?: boolean | null
}

export interface UpdatePlannedScopeRequest {
  plannedIssueKeys: string[]
}

export interface RecordRolloverRequest {
  issueKey: string
  toSprintId: number
  notes?: string | null
  moveInJira?: boolean | null
}
