import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ApiError, listTeams } from '../api'
import type { TeamResponse } from '../api/types'
import { PageEmptyState, PageErrorState, PageHeader, PageLoadingState } from '../components/common'
import { TeamCard } from '../components/selectors/TeamCard'
import { useAppContext } from '../context/AppContext'

export function TeamSelectionPage() {
  const navigate = useNavigate()
  const { setTeam } = useAppContext()
  const [teams, setTeams] = useState<TeamResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const loadTeams = useCallback(async () => {
    setLoading(true)
    setError(null)

    try {
      const data = await listTeams()
      setTeams(data)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Failed to load teams.')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void loadTeams()
  }, [loadTeams])

  const handleSelectTeam = (team: TeamResponse) => {
    setTeam({ id: team.id, name: team.name, code: team.code })
    navigate(`/teams/${team.id}/pods`)
  }

  if (loading) {
    return (
      <>
        <PageHeader
          title="Select Team"
          description="Choose a product or service to continue sprint planning."
        />
        <PageLoadingState message="Loading teams..." />
      </>
    )
  }

  if (error) {
    return (
      <>
        <PageHeader
          title="Select Team"
          description="Choose a product or service to continue sprint planning."
        />
        <PageErrorState message={error} onRetry={loadTeams} />
      </>
    )
  }

  if (teams.length === 0) {
    return (
      <>
        <PageHeader
          title="Select Team"
          description="Choose a product or service to continue sprint planning."
        />
        <PageEmptyState
          title="No teams available"
          description="Active teams will appear here once they are configured in the backend."
        />
      </>
    )
  }

  return (
    <div>
      <PageHeader
        title="Select Team"
        description="Choose a product or service to view its pods and planning workflows."
      />

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
        {teams.map((team) => (
          <TeamCard key={team.id} team={team} onSelect={handleSelectTeam} />
        ))}
      </div>
    </div>
  )
}
