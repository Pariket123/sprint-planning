import { useCallback, useEffect, useState } from 'react'
import { Navigate, Outlet, useParams } from 'react-router-dom'
import { ApiError, getPod, listTeams } from '../../api'
import type { PodResponse } from '../../api/types'
import { PageErrorState, PageLoadingState } from '../../components/common'
import { useAppContext } from '../../context/AppContext'
import { PodRouteContext } from './podRouteContext'

export function PodRouteGuard() {
  const { podId } = useParams<{ podId: string }>()
  const { setPod, syncTeam } = useAppContext()
  const [pod, setPodState] = useState<PodResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const validatePod = useCallback(async () => {
    if (!podId) {
      setLoading(false)
      return
    }

    setLoading(true)
    setError(null)

    try {
      const podDetails = await getPod(podId)
      setPod({ id: podDetails.id, name: podDetails.name, code: podDetails.code })

      const teams = await listTeams()
      const team = teams.find((item) => item.id === podDetails.teamId)
      if (team) {
        syncTeam({ id: team.id, name: team.name, code: team.code })
      }

      setPodState(podDetails)
    } catch (err) {
      setPodState(null)
      setError(err instanceof ApiError ? err.message : 'Failed to load pod.')
    } finally {
      setLoading(false)
    }
  }, [podId, setPod, syncTeam])

  useEffect(() => {
    void validatePod()
  }, [validatePod])

  if (!podId) {
    return <Navigate to="/teams" replace />
  }

  if (loading) {
    return <PageLoadingState message="Loading pod context..." />
  }

  if (error || !pod) {
    return (
      <PageErrorState
        message={error ?? 'Pod not found.'}
        onRetry={() => {
          void validatePod()
        }}
      />
    )
  }

  return (
    <PodRouteContext.Provider value={{ pod }}>
      <Outlet />
    </PodRouteContext.Provider>
  )
}

export function PodsIndexRedirect() {
  const { selectedPodId, hasPod } = useAppContext()

  if (hasPod && selectedPodId) {
    return <Navigate to={`/pods/${selectedPodId}`} replace />
  }

  return <Navigate to="/teams" replace />
}
