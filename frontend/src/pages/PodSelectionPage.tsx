import { useCallback, useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { ArrowLeft } from 'lucide-react'
import { ApiError, listPods, listTeams } from '../api'
import type { PodResponse } from '../api/types'
import { PageEmptyState, PageErrorState, PageHeader, PageLoadingState } from '../components/common'
import { PodCard } from '../components/selectors/PodCard'
import { useAppContext } from '../context/AppContext'

export function PodSelectionPage() {
  const navigate = useNavigate()
  const { teamId } = useParams<{ teamId: string }>()
  const { setTeam, setPod, selectedTeamId, selectedTeamName, clearAll } = useAppContext()
  const [pods, setPods] = useState<PodResponse[]>([])
  const [teamName, setTeamName] = useState<string | null>(selectedTeamName)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const loadPods = useCallback(async () => {
    if (!teamId) {
      setError('Team is required.')
      setLoading(false)
      return
    }

    setLoading(true)
    setError(null)

    try {
      const [teams, podList] = await Promise.all([listTeams(), listPods(teamId)])
      const team = teams.find((item) => item.id === teamId)

      if (!team) {
        setError('Team not found. Return to team selection and try again.')
        setPods([])
        return
      }

      if (selectedTeamId !== team.id) {
        setTeam({ id: team.id, name: team.name, code: team.code })
      }

      setTeamName(team.name)
      setPods(podList)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Failed to load pods.')
    } finally {
      setLoading(false)
    }
  }, [teamId, selectedTeamId, setTeam])

  useEffect(() => {
    void loadPods()
  }, [loadPods])

  const handleSelectPod = (pod: PodResponse) => {
    setPod({ id: pod.id, name: pod.name, code: pod.code })
    navigate(`/pods/${pod.id}`)
  }

  const handleChangeTeam = () => {
    clearAll()
    navigate('/teams')
  }

  if (!teamId) {
    return (
      <PageErrorState
        message="Missing team in the URL."
        onRetry={() => navigate('/teams')}
      />
    )
  }

  if (loading) {
    return (
      <>
        <PageHeader
          title="Select Pod"
          description={
            teamName
              ? `Choose a module inside ${teamName}.`
              : 'Choose a pod or module for this team.'
          }
        />
        <PageLoadingState message="Loading pods..." />
      </>
    )
  }

  if (error) {
    return (
      <>
        <PageHeader
          title="Select Pod"
          description="Choose a pod or module to open the planning dashboard."
        />
        <PageErrorState message={error} onRetry={loadPods} />
      </>
    )
  }

  return (
    <div>
      <PageHeader
        title="Select Pod"
        description={
          teamName
            ? `Choose a module inside ${teamName}.`
            : 'Choose a pod or module to open the planning dashboard.'
        }
        actions={
          <button
            type="button"
            onClick={handleChangeTeam}
            className="btn-secondary"
          >
            <ArrowLeft className="h-4 w-4" aria-hidden="true" />
            Change team
          </button>
        }
      />

      {pods.length === 0 ? (
        <PageEmptyState
          title="No pods available"
          description="Active pods for this team will appear here once configured."
          action={
            <button
              type="button"
              onClick={handleChangeTeam}
              className="btn-primary"
            >
              Back to teams
            </button>
          }
        />
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
          {pods.map((pod) => (
            <PodCard key={pod.id} pod={pod} onSelect={handleSelectPod} />
          ))}
        </div>
      )}
    </div>
  )
}
