import { Navigate, createBrowserRouter } from 'react-router-dom'
import { AppLayout } from '../components/layout'
import { AnalyzeSprintPage } from '../pages/AnalyzeSprintPage'
import { DebugSettingsPage } from '../pages/DebugSettingsPage'
import { PodDashboardPage } from '../pages/PodDashboardPage'
import { PodSelectionPage } from '../pages/PodSelectionPage'
import { TeamSelectionPage } from '../pages/TeamSelectionPage'
import { PlanSprintPage } from '../pages/PlanSprintPage'
import { ReleasePage } from '../pages/ReleasePage'
import { PodRouteGuard, PodsIndexRedirect } from './guards'

export const router = createBrowserRouter([
  {
    path: '/',
    element: <AppLayout />,
    children: [
      { index: true, element: <Navigate to="teams" replace /> },
      { path: 'teams', element: <TeamSelectionPage /> },
      { path: 'teams/:teamId/pods', element: <PodSelectionPage /> },
      { path: 'pods', element: <PodsIndexRedirect /> },
      {
        path: 'pods/:podId',
        element: <PodRouteGuard />,
        children: [
          { index: true, element: <PodDashboardPage /> },
          {
            path: 'analyze',
            element: <AnalyzeSprintPage />,
            handle: { section: 'Analyze Sprint' },
          },
          {
            path: 'plan',
            element: <PlanSprintPage />,
            handle: { section: 'Plan Sprint' },
          },
          {
            path: 'releases',
            element: <ReleasePage />,
            handle: { section: 'Create/View Release' },
          },
        ],
      },
      { path: 'settings', element: <DebugSettingsPage /> },
    ],
  },
  { path: '*', element: <Navigate to="/teams" replace /> },
])
