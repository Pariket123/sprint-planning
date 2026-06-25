import type { IssueSearchFilters } from '../../api/types'
import { TagInput } from '../common/TagInput'
import { parseNumberListInput, parseTagInput } from '../../utils/listInput'

export interface IssueFilterFormState {
  issueTypes: string
  statuses: string
  domains: string
  priorities: string
  assigneeIds: string
  fixVersions: string
  fixVersionExcludes: string
  issueKeys: string
  sprintIds: string
  labels: string
  components: string
}

export const emptyIssueFilterForm: IssueFilterFormState = {
  issueTypes: '',
  statuses: '',
  domains: '',
  priorities: '',
  assigneeIds: '',
  fixVersions: '',
  fixVersionExcludes: '',
  issueKeys: '',
  sprintIds: '',
  labels: '',
  components: '',
}

export function issueFilterFormToRequest(form: IssueFilterFormState): IssueSearchFilters {
  const filters: IssueSearchFilters = {
    issueTypes: parseTagInput(form.issueTypes),
    statuses: parseTagInput(form.statuses),
    domains: parseTagInput(form.domains),
    priorities: parseTagInput(form.priorities),
    assigneeIds: parseTagInput(form.assigneeIds),
    fixVersions: parseTagInput(form.fixVersions),
    fixVersionExcludes: parseTagInput(form.fixVersionExcludes),
    issueKeys: parseTagInput(form.issueKeys),
    sprintIds: parseNumberListInput(form.sprintIds),
    labels: parseTagInput(form.labels),
    components: parseTagInput(form.components),
  }

  return Object.fromEntries(
    Object.entries(filters).filter(([, value]) => {
      if (Array.isArray(value)) {
        return value.length > 0
      }
      return value !== null && value !== undefined
    }),
  ) as IssueSearchFilters
}

interface FilterPanelProps {
  value: IssueFilterFormState
  onChange: (value: IssueFilterFormState) => void
}

export function FilterPanel({ value, onChange }: FilterPanelProps) {
  const update = (field: keyof IssueFilterFormState, fieldValue: string) => {
    onChange({ ...value, [field]: fieldValue })
  }

  return (
    <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
      <TagInput
        id="filter-issue-types"
        label="Issue types"
        value={value.issueTypes}
        onChange={(next) => update('issueTypes', next)}
      />
      <TagInput
        id="filter-statuses"
        label="Statuses"
        value={value.statuses}
        onChange={(next) => update('statuses', next)}
      />
      <TagInput
        id="filter-domains"
        label="Domains"
        value={value.domains}
        onChange={(next) => update('domains', next)}
      />
      <TagInput
        id="filter-priorities"
        label="Priorities"
        value={value.priorities}
        onChange={(next) => update('priorities', next)}
      />
      <TagInput
        id="filter-assignees"
        label="Assignee IDs"
        value={value.assigneeIds}
        onChange={(next) => update('assigneeIds', next)}
      />
      <TagInput
        id="filter-fix-versions"
        label="Fix versions"
        value={value.fixVersions}
        onChange={(next) => update('fixVersions', next)}
      />
      <TagInput
        id="filter-fix-version-excludes"
        label="Fix version excludes"
        value={value.fixVersionExcludes}
        onChange={(next) => update('fixVersionExcludes', next)}
      />
      <TagInput
        id="filter-issue-keys"
        label="Issue keys"
        value={value.issueKeys}
        onChange={(next) => update('issueKeys', next)}
      />
      <TagInput
        id="filter-sprint-ids"
        label="Sprint IDs"
        value={value.sprintIds}
        onChange={(next) => update('sprintIds', next)}
        hint="Comma-separated numbers"
      />
      <TagInput
        id="filter-labels"
        label="Labels"
        value={value.labels}
        onChange={(next) => update('labels', next)}
      />
      <TagInput
        id="filter-components"
        label="Components"
        value={value.components}
        onChange={(next) => update('components', next)}
      />
    </div>
  )
}
