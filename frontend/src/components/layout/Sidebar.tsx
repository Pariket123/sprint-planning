import type { ComponentType } from 'react'
import { NavLink } from 'react-router-dom'
import {
  BarChart3,
  CalendarRange,
  LayoutDashboard,
  Package,
  Settings,
  Users,
} from 'lucide-react'
import { useAppContext } from '../../context/AppContext'

interface NavItem {
  label: string
  to: string
  icon: ComponentType<{ className?: string }>
  end?: boolean
}

function navLinkClass({ isActive }: { isActive: boolean }) {
  return [
    'flex items-center gap-3 border-l-2 px-3 py-2 text-sm font-medium text-white transition',
    isActive
      ? 'border-brand-600 bg-white/[0.08]'
      : 'border-transparent hover:bg-white/[0.06]',
  ].join(' ')
}

export function Sidebar() {
  const { hasTeam, hasPod, selectedTeamId, selectedPodId } = useAppContext()

  const navItems: NavItem[] = (() => {
    if (!hasTeam) {
      return [{ label: 'Select Team', to: '/teams', icon: Users, end: true }]
    }

    if (!hasPod) {
      return [
        {
          label: 'Select Pod',
          to: `/teams/${selectedTeamId}/pods`,
          icon: Package,
          end: true,
        },
      ]
    }

    const podBase = `/pods/${selectedPodId}`
    return [
      { label: 'Pod Dashboard', to: podBase, icon: LayoutDashboard, end: true },
      { label: 'Analyze Sprint', to: `${podBase}/analyze`, icon: BarChart3 },
      { label: 'Plan Sprint', to: `${podBase}/plan`, icon: CalendarRange },
      { label: 'Create/View Release', to: `${podBase}/releases`, icon: Package },
    ]
  })()

  return (
    <aside className="flex h-screen w-64 shrink-0 flex-col overflow-hidden bg-[#0A0A0A]">
      <div className="border-b border-white/10 px-5 py-5">
        <p className="text-xs font-semibold uppercase tracking-wider text-white">
          Sprint Planning
        </p>
        <h2 className="mt-1 text-lg font-semibold text-white">Planning Console</h2>
      </div>

      <nav className="flex-1 space-y-1 overflow-y-auto px-3 py-4" aria-label="Main navigation">
        {navItems.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            end={item.end}
            className={navLinkClass}
          >
            <item.icon className="h-4 w-4 shrink-0 text-white" aria-hidden="true" />
            <span>{item.label}</span>
          </NavLink>
        ))}
      </nav>

      <div className="border-t border-white/10 px-3 py-3">
        <NavLink to="/settings" className={navLinkClass}>
          <Settings className="h-4 w-4 shrink-0 text-white" aria-hidden="true" />
          <span>Debug / Settings</span>
        </NavLink>
      </div>

      <div className="border-t border-white/10 px-4 py-4">
        <p className="text-xs text-white/70">
          {!hasTeam && 'Start by selecting a team or product.'}
          {hasTeam && !hasPod && 'Choose a pod to open planning workflows.'}
          {hasTeam && hasPod && 'Work inside the selected pod context.'}
        </p>
      </div>
    </aside>
  )
}
