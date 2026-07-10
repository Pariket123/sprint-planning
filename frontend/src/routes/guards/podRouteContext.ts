import { createContext, useContext } from 'react'
import type { PodResponse } from '../../api/types'

export interface PodRouteContextValue {
  pod: PodResponse
}

export const PodRouteContext = createContext<PodRouteContextValue | null>(null)

export function usePodRouteContext(): PodRouteContextValue {
  const context = useContext(PodRouteContext)
  if (!context) {
    throw new Error('usePodRouteContext must be used within PodRouteGuard')
  }
  return context
}
export function useOptionalPodRouteContext(): PodRouteContextValue | null {
  return useContext(PodRouteContext)
}

