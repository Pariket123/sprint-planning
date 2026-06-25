import { useMatches } from 'react-router-dom'
import { getRouteSection } from './routeHandles'

export function useRouteSection(): string | undefined {
  const matches = useMatches()
  return getRouteSection(matches)
}
