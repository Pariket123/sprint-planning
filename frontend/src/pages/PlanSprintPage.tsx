import { useCallback, useEffect, useRef, useState } from 'react'
import { useParams } from 'react-router-dom'
import {
  ApiError,
  getPlanning,
  listSprints,
  updateCapacity,
  updateCapacityAllocation,
  updateLeaves,
  updateOverrides,
  validatePlanning,
} from '../api'
import type {
  PlanningSummaryDto,
  PlanningValidationResultDto,
  PlanningViewDto,
  SprintView,
} from '../api/types'
import {
  AnalyticsSummaryCard,
  PageEmptyState,
  PageErrorState,
  PageHeader,
  PageLoadingState,
} from '../components/common'
import { BacklogTab } from '../components/planning/BacklogTab'
import { CapacityEditor } from '../components/planning/CapacityEditor'
import { CapacityAllocationEditor } from '../components/planning/CapacityAllocationEditor'
import { DomainMetricsTable } from '../components/planning/DomainMetricsTable'
import { IssuesTab } from '../components/planning/IssuesTab'
import { LeavesEditor } from '../components/planning/LeavesEditor'
import { OverridesEditor } from '../components/planning/OverridesEditor'
import { PlannedScopeTab } from '../components/planning/PlannedScopeTab'
import { RiskBadge } from '../components/planning/RiskBadge'
import { RolloverTab } from '../components/planning/RolloverTab'
import { SprintSelector } from '../components/selectors/SprintSelector'
import { useAppContext } from '../context/AppContext'
import { formatInstant, formatSprintState, formatStoryPoints } from '../utils/format'
import { buildPlanningSummary } from '../utils/planningSummary'
import { PlanningCapacityGuidance } from '../components/planning/PlanningCapacityGuidance'

type PlanTab =
  | 'overview'
  | 'capacity'
  | 'issues'
  | 'backlog'
  | 'planned'
  | 'rollover'
  | 'summary'

export function PlanSprintPage() {
  const { podId } = useParams<{ podId: string }>()
  const { selectedSprintId, setSprintId } = useAppContext()
  const [activeTab, setActiveTab] = useState<PlanTab>('overview')
  const [sprints, setSprints] = useState<SprintView[]>([])
  const [planning, setPlanning] = useState<PlanningViewDto | null>(null)
  const [summary, setSummary] = useState<PlanningSummaryDto | null>(null)
  const [validation, setValidation] = useState<PlanningValidationResultDto | null>(null)
  const [sprintsLoading, setSprintsLoading] = useState(true)
  const [planningLoading, setPlanningLoading] = useState(false)
  const [validating, setValidating] = useState(false)
  const [sprintsError, setSprintsError] = useState<string | null>(null)
  const [planningError, setPlanningError] = useState<string | null>(null)
  const planningRequestId = useRef(0)

  const loadSprints = useCallback(async () => {
    if (!podId) {
      setSprintsError('Pod is required.')
      setSprintsLoading(false)
      return
    }

    setSprintsLoading(true)
    setSprintsError(null)

    try {
      const sprintList = await listSprints(podId)
      setSprints(sprintList)
    } catch (err) {
      setSprints([])
      setSprintsError(err instanceof ApiError ? err.message : 'Failed to load sprints.')
    } finally {
      setSprintsLoading(false)
    }
  }, [podId])

  const loadPlanning = useCallback(async () => {
    if (!podId || selectedSprintId === null) {
      return
    }

    const requestId = planningRequestId.current + 1
    planningRequestId.current = requestId

    setPlanningLoading(true)
    setPlanningError(null)

    try {
      const planningData = await getPlanning(podId, selectedSprintId)
      if (planningRequestId.current !== requestId) {
        return
      }

      setPlanning(planningData)
      setSummary(buildPlanningSummary(planningData))
      setValidation(null)
    } catch (err) {
      if (planningRequestId.current !== requestId) {
        return
      }

      setPlanning(null)
      setSummary(null)
      setPlanningError(err instanceof ApiError ? err.message : 'Failed to load planning view.')
    } finally {
      if (planningRequestId.current === requestId) {
        setPlanningLoading(false)
      }
    }
  }, [podId, selectedSprintId])

  useEffect(() => {
    void loadSprints()
  }, [loadSprints])

  useEffect(() => {
    if (sprintsLoading || sprints.length === 0) {
      return
    }

    const isValidSelection =
      selectedSprintId !== null && sprints.some((sprint) => sprint.id === selectedSprintId)

    if (!isValidSelection) {
      setSprintId(sprints[0].id)
      return
    }

    void loadPlanning()
  }, [sprints, sprintsLoading, selectedSprintId, setSprintId, loadPlanning])

  useEffect(() => {
    if (selectedSprintId === null) {
      setPlanning(null)
      setSummary(null)
      setPlanningError(null)
    }
  }, [selectedSprintId])

  const handleValidate = async () => {
    if (!podId || selectedSprintId === null) {
      return
    }

    setValidating(true)
    try {
      const result = await validatePlanning(podId, selectedSprintId)
      setValidation(result)
    } catch (err) {
      setValidation(null)
      setPlanningError(err instanceof ApiError ? err.message : 'Failed to validate planning.')
    } finally {
      setValidating(false)
    }
  }

  if (sprintsLoading) {
    return (
      <>
        <PageHeader title="Plan Sprint" description="Capacity planning, issues, and validation." />
        <PageLoadingState message="Loading sprints..." />
      </>
    )
  }

  if (sprintsError) {
    return (
      <>
        <PageHeader title="Plan Sprint" description="Capacity planning, issues, and validation." />
        <PageErrorState message={sprintsError} onRetry={loadSprints} />
      </>
    )
  }

  if (sprints.length === 0) {
    return (
      <>
        <PageHeader title="Plan Sprint" description="Capacity planning, issues, and validation." />
        <PageEmptyState
          title="No sprints found"
          description="Sprints from Jira will appear here when available for this pod."
        />
      </>
    )
  }

  return (
    <div className="space-y-6">
      <PageHeader
        title="Plan Sprint"
        description="Review capacity, issues, and validation before committing sprint scope."
      />

      <SprintSelector
        sprints={sprints}
        selectedSprintId={selectedSprintId}
        onChange={setSprintId}
        description="Select a sprint to review capacity, issues, and validation."
      />

      {planningLoading && <PageLoadingState message="Loading planning view..." />}

      {planningError && !planningLoading && (
        <PageErrorState message={planningError} onRetry={loadPlanning} />
      )}

      {planning && summary && !planningLoading && !planningError && podId && selectedSprintId !== null && (
        <>
          <div className="border-b border-gray-200">
            <nav className="-mb-px flex flex-wrap gap-4" aria-label="Plan sprint tabs">
              <TabButton active={activeTab === 'overview'} onClick={() => setActiveTab('overview')} label="Overview" />
              <TabButton active={activeTab === 'capacity'} onClick={() => setActiveTab('capacity')} label="Capacity" />
              <TabButton active={activeTab === 'issues'} onClick={() => setActiveTab('issues')} label="Issues" />
              <TabButton active={activeTab === 'backlog'} onClick={() => setActiveTab('backlog')} label="Backlog" />
              <TabButton active={activeTab === 'planned'} onClick={() => setActiveTab('planned')} label="Planned Scope" />
              <TabButton active={activeTab === 'rollover'} onClick={() => setActiveTab('rollover')} label="Rollover" />
              <TabButton active={activeTab === 'summary'} onClick={() => setActiveTab('summary')} label="Summary & Validation" />
            </nav>
          </div>

          {activeTab === 'overview' && (
            <OverviewTab planning={planning} summary={summary} />
          )}

          {activeTab === 'capacity' && (
            <div className="space-y-6">
              <section className="rounded-xl border border-gray-200 bg-white p-5 shadow-sm">
                <h2 className="text-sm font-semibold text-gray-900">Capacity</h2>
                <p className="mt-1 text-sm text-gray-600">Add each team member with their domain and bandwidth.</p>
                <div className="mt-4">
                  <CapacityEditor
                    initial={planning.capacity ?? []}
                    onSave={async (capacity) => {
                      await updateCapacity(podId, selectedSprintId, { capacity })
                      await loadPlanning()
                    }}
                  />
                </div>
              </section>

              <section className="rounded-xl border border-gray-200 bg-white p-5 shadow-sm">
                <h2 className="text-sm font-semibold text-gray-900">Leaves & holidays</h2>
                <div className="mt-4">
                  <LeavesEditor
                    initial={planning.leaves ?? []}
                    onSave={async (leaves) => {
                      await updateLeaves(podId, selectedSprintId, { leaves })
                      await loadPlanning()
                    }}
                  />
                </div>
              </section>

              <section className="rounded-xl border border-gray-200 bg-white p-5 shadow-sm">
                <h2 className="text-sm font-semibold text-gray-900">Overrides</h2>
                <p className="mt-1 text-sm text-gray-600">
                  Include or exclude issues from planning calculations.
                </p>
                <div className="mt-4">
                  <OverridesEditor
                    initial={planning.overrides ?? []}
                    onSave={async (overrides) => {
                      await updateOverrides(podId, selectedSprintId, { overrides })
                      await loadPlanning()
                    }}
                  />
                </div>
              </section>

              <section className="rounded-xl border border-gray-200 bg-white p-5 shadow-sm">
                <h2 className="text-sm font-semibold text-gray-900">Capacity allocation</h2>
                <p className="mt-1 text-sm text-gray-600">
                  Split available capacity between roadmap work and bug/support work by domain.
                </p>
                <div className="mt-4">
                  <CapacityAllocationEditor
                    table={planning.capacityAllocationTable}
                    initialPercents={planning.capacityAllocation ?? []}
                    onSave={async (capacityAllocation) => {
                      await updateCapacityAllocation(podId, selectedSprintId, { capacityAllocation })
                      await loadPlanning()
                    }}
                  />
                </div>
              </section>

              {planning.domainMetrics && planning.domainMetrics.length > 0 && (
                <section className="rounded-xl border border-gray-200 bg-white p-5 shadow-sm">
                  <h2 className="text-sm font-semibold text-gray-900">Domain metrics</h2>
                  <p className="mt-1 text-sm text-gray-600">
                    BE, UI, AI, and QA utilization is measured against planned roadmap capacity.
                  </p>
                  <div className="mt-4">
                    <DomainMetricsTable metrics={planning.domainMetrics} />
                  </div>
                </section>
              )}
            </div>
          )}

          {activeTab === 'issues' && (
            <section className="rounded-xl border border-gray-200 bg-white p-5 shadow-sm">
              <IssuesTab
                podId={podId}
                jiraSprintId={selectedSprintId}
                planning={planning}
                onPlanningUpdated={loadPlanning}
              />
            </section>
          )}

          {activeTab === 'backlog' && (
            <section className="rounded-xl border border-gray-200 bg-white p-5 shadow-sm">
              <BacklogTab
                podId={podId}
                jiraSprintId={selectedSprintId}
                onPlanningUpdated={loadPlanning}
              />
            </section>
          )}

          {activeTab === 'planned' && (
            <PlannedScopeTab
              podId={podId}
              jiraSprintId={selectedSprintId}
              selectedIssueKeys={planning.selectedIssueKeys ?? []}
              onPlanningUpdated={loadPlanning}
            />
          )}

          {activeTab === 'rollover' && (
            <RolloverTab
              podId={podId}
              jiraSprintId={selectedSprintId}
              sprints={sprints}
              onPlanningUpdated={loadPlanning}
            />
          )}

          {activeTab === 'summary' && (
            <SummaryTab
              summary={summary}
              validation={validation}
              validating={validating}
              onValidate={() => void handleValidate()}
            />
          )}
        </>
      )}
    </div>
  )
}

function OverviewTab({
  planning,
  summary,
}: {
  planning: PlanningViewDto
  summary: PlanningSummaryDto
}) {
  const sprint = planning.sprint

  return (
    <div className="space-y-6">
      <section className="rounded-xl border border-gray-200 bg-white p-5 shadow-sm">
        <h2 className="text-sm font-semibold text-gray-900">Sprint details</h2>
        <dl className="mt-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-4 text-sm">
          <Metric label="Sprint" value={sprint.name} />
          <Metric label="State" value={formatSprintState(sprint.state)} />
          <Metric label="Start" value={formatInstant(sprint.startDate)} />
          <Metric label="End" value={formatInstant(sprint.endDate)} />
        </dl>
      </section>

      <section>
        <div className="mb-4 flex items-center justify-between gap-3">
          <h2 className="text-sm font-semibold text-gray-900">Planning summary</h2>
          <RiskBadge risk={summary.riskLevel} />
        </div>
        <PlanningCapacityGuidance summary={summary} />
        <div className="mt-4 grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
          <AnalyticsSummaryCard
            label="Total available capacity"
            value={formatStoryPoints(summary.totalAvailableCapacity)}
          />
          <AnalyticsSummaryCard label="Total rollover" value={formatStoryPoints(summary.totalRollover)} />
          <AnalyticsSummaryCard
            label="Roadmap capacity"
            value={formatStoryPoints(summary.totalRoadmapCapacity)}
          />
          <AnalyticsSummaryCard
            label="Selected story points"
            value={formatStoryPoints(summary.totalSelectedStoryPoints)}
          />
          <AnalyticsSummaryCard label="Selected issues" value={summary.totalSelectedIssueCount} />
          <AnalyticsSummaryCard
            label="Committed story points"
            value={formatStoryPoints(summary.totalCommittedStoryPoints)}
          />
          <AnalyticsSummaryCard
            label="Committed issues"
            value={summary.totalCommittedIssueCount}
          />
          <AnalyticsSummaryCard
            label="Planned issues"
            value={(planning.plannedIssueKeys ?? []).length}
          />
          <AnalyticsSummaryCard
            label="Rollover issues"
            value={(planning.rolloverIssues ?? []).length}
          />
        </div>
      </section>

      <section className="rounded-xl border border-gray-200 bg-white p-5 shadow-sm">
        <h2 className="text-sm font-semibold text-gray-900">Domain metrics</h2>
        <div className="mt-4">
          <DomainMetricsTable metrics={summary.domainMetrics} />
        </div>
      </section>
    </div>
  )
}

function SummaryTab({
  summary,
  validation,
  validating,
  onValidate,
}: {
  summary: PlanningSummaryDto
  validation: PlanningValidationResultDto | null
  validating: boolean
  onValidate: () => void
}) {
  return (
    <div className="space-y-6">
      <section>
        <div className="mb-4 flex items-center justify-between gap-3">
          <div>
            <h2 className="text-sm font-semibold text-gray-900">Capacity vs committed</h2>
            <p className="mt-1 text-sm text-gray-600">
              Risk and utilization are based on committed story points vs roadmap capacity.
            </p>
          </div>
          <RiskBadge risk={summary.riskLevel} />
        </div>
        <PlanningCapacityGuidance summary={summary} />
        <div className="mt-4 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          <AnalyticsSummaryCard
            label="Total available capacity"
            value={formatStoryPoints(summary.totalAvailableCapacity)}
          />
          <AnalyticsSummaryCard label="Total rollover" value={formatStoryPoints(summary.totalRollover)} />
          <AnalyticsSummaryCard
            label="Roadmap capacity"
            value={formatStoryPoints(summary.totalRoadmapCapacity)}
          />
          <AnalyticsSummaryCard
            label="Committed story points"
            value={formatStoryPoints(summary.totalCommittedStoryPoints)}
          />
          <AnalyticsSummaryCard
            label="Selected story points"
            value={formatStoryPoints(summary.totalSelectedStoryPoints)}
          />
          <AnalyticsSummaryCard label="Committed issues" value={summary.totalCommittedIssueCount} />
          <AnalyticsSummaryCard label="Selected issues" value={summary.totalSelectedIssueCount} />
        </div>
      </section>

      <section className="rounded-xl border border-gray-200 bg-white p-5 shadow-sm">
        <h2 className="text-sm font-semibold text-gray-900">Domain metrics</h2>
        <div className="mt-4">
          <DomainMetricsTable metrics={summary.domainMetrics} />
        </div>
      </section>

      <section className="rounded-xl border border-gray-200 bg-white p-5 shadow-sm">
        <div className="flex items-center justify-between gap-3">
          <h2 className="text-sm font-semibold text-gray-900">Validation</h2>
          <button
            type="button"
            onClick={onValidate}
            disabled={validating}
            className="btn-primary"
          >
            {validating ? 'Validating...' : 'Run validation'}
          </button>
        </div>

        <div className="mt-4">
          {!validation && (
            <p className="text-sm text-gray-500">
              Run validation to check capacity risks and planning warnings.
            </p>
          )}

          {validation && validation.warnings.length === 0 && (
            <div className="rounded-md border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-800">
              No validation warnings. Risk level: {validation.riskLevel}.
            </div>
          )}

          {validation && validation.warnings.length > 0 && (
            <ul className="space-y-2">
              {validation.warnings.map((warning, index) => (
                <li
                  key={`${warning.code}-${index}`}
                  className="rounded-md border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-900"
                >
                  <span className="font-medium">{warning.code}</span>
                  {warning.domain && <span className="ml-2 text-amber-700">({warning.domain})</span>}
                  <p className="mt-1">{warning.message}</p>
                </li>
              ))}
            </ul>
          )}
        </div>
      </section>
    </div>
  )
}

function TabButton({
  active,
  onClick,
  label,
}: {
  active: boolean
  onClick: () => void
  label: string
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`border-b-2 px-1 py-3 text-sm font-medium transition ${
        active
          ? 'border-brand-600 text-brand-600'
          : 'border-transparent text-gray-500 hover:border-gray-200 hover:text-gray-600'
      }`}
    >
      {label}
    </button>
  )
}

function Metric({ label, value }: { label: string; value: string | number }) {
  return (
    <div className="rounded-md border border-gray-100 bg-gray-50 px-4 py-3">
      <dt className="text-xs font-medium uppercase tracking-wide text-gray-500">{label}</dt>
      <dd className="mt-1 text-sm font-medium text-gray-900">{value}</dd>
    </div>
  )
}
