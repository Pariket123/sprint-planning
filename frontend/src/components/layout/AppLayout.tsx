import { Outlet } from 'react-router-dom'
import { Header } from './Header'
import { Sidebar } from './Sidebar'
import { useRouteSection } from '../../routes/useRouteSection'

export function AppLayout() {
  const section = useRouteSection()

  return (
    <div className="flex h-screen overflow-hidden bg-gray-50">
      <Sidebar />
      <div className="flex min-h-0 min-w-0 flex-1 flex-col overflow-y-auto">
        <div className="aurora-accent" aria-hidden="true" />
        <Header section={section} />
        <main className="flex-1 px-6 py-6">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
