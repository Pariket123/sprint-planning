import { Navigate, Outlet, useParams } from 'react-router-dom'

export function TeamRouteGuard() {
  const { teamId } = useParams<{ teamId: string }>()

  if (!teamId) {
    return <Navigate to="/teams" replace />
  }

  return <Outlet />
}
