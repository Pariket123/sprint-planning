import {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useState,
  type ReactNode,
} from 'react'

const STORAGE_KEYS = {
  teamId: 'sprint-planning:teamId',
  teamName: 'sprint-planning:teamName',
  podId: 'sprint-planning:podId',
  podName: 'sprint-planning:podName',
  sprintId: 'sprint-planning:sprintId',
} as const

function readStorage(key: string): string | null {
  try {
    return localStorage.getItem(key)
  } catch {
    return null
  }
}

function writeStorage(key: string, value: string | null): void {
  try {
    if (value === null) {
      localStorage.removeItem(key)
    } else {
      localStorage.setItem(key, value)
    }
  } catch {
    // Ignore storage failures (private browsing, quota, etc.)
  }
}

export interface TeamSelection {
  id: string
  name: string
  code?: string
}

export interface PodSelection {
  id: string
  name: string
  code?: string
}

interface AppContextValue {
  selectedTeamId: string | null
  selectedTeamName: string | null
  selectedPodId: string | null
  selectedPodName: string | null
  selectedSprintId: number | null
  hasTeam: boolean
  hasPod: boolean
  setTeam: (team: TeamSelection) => void
  syncTeam: (team: TeamSelection) => void
  setPod: (pod: PodSelection) => void
  setSprintId: (sprintId: number | null) => void
  clearPod: () => void
  clearAll: () => void
}

const AppContext = createContext<AppContextValue | null>(null)

export function AppProvider({ children }: { children: ReactNode }) {
  const [selectedTeamId, setSelectedTeamId] = useState<string | null>(
    () => readStorage(STORAGE_KEYS.teamId),
  )
  const [selectedTeamName, setSelectedTeamName] = useState<string | null>(
    () => readStorage(STORAGE_KEYS.teamName),
  )
  const [selectedPodId, setSelectedPodId] = useState<string | null>(
    () => readStorage(STORAGE_KEYS.podId),
  )
  const [selectedPodName, setSelectedPodName] = useState<string | null>(
    () => readStorage(STORAGE_KEYS.podName),
  )
  const [selectedSprintId, setSelectedSprintId] = useState<number | null>(() => {
    const stored = readStorage(STORAGE_KEYS.sprintId)
    if (!stored) {
      return null
    }
    const parsed = Number(stored)
    return Number.isFinite(parsed) ? parsed : null
  })

  const setTeam = useCallback((team: TeamSelection) => {
    setSelectedTeamId(team.id)
    setSelectedTeamName(team.name)
    writeStorage(STORAGE_KEYS.teamId, team.id)
    writeStorage(STORAGE_KEYS.teamName, team.name)

    setSelectedPodId(null)
    setSelectedPodName(null)
    setSelectedSprintId(null)
    writeStorage(STORAGE_KEYS.podId, null)
    writeStorage(STORAGE_KEYS.podName, null)
    writeStorage(STORAGE_KEYS.sprintId, null)
  }, [])

  const syncTeam = useCallback((team: TeamSelection) => {
    setSelectedTeamId(team.id)
    setSelectedTeamName(team.name)
    writeStorage(STORAGE_KEYS.teamId, team.id)
    writeStorage(STORAGE_KEYS.teamName, team.name)
  }, [])

  const setPod = useCallback((pod: PodSelection) => {
    setSelectedPodId(pod.id)
    setSelectedPodName(pod.name)
    writeStorage(STORAGE_KEYS.podId, pod.id)
    writeStorage(STORAGE_KEYS.podName, pod.name)

    setSelectedSprintId(null)
    writeStorage(STORAGE_KEYS.sprintId, null)
  }, [])

  const setSprintId = useCallback((sprintId: number | null) => {
    setSelectedSprintId(sprintId)
    writeStorage(
      STORAGE_KEYS.sprintId,
      sprintId === null ? null : String(sprintId),
    )
  }, [])

  const clearPod = useCallback(() => {
    setSelectedPodId(null)
    setSelectedPodName(null)
    setSelectedSprintId(null)
    writeStorage(STORAGE_KEYS.podId, null)
    writeStorage(STORAGE_KEYS.podName, null)
    writeStorage(STORAGE_KEYS.sprintId, null)
  }, [])

  const clearAll = useCallback(() => {
    setSelectedTeamId(null)
    setSelectedTeamName(null)
    setSelectedPodId(null)
    setSelectedPodName(null)
    setSelectedSprintId(null)
    writeStorage(STORAGE_KEYS.teamId, null)
    writeStorage(STORAGE_KEYS.teamName, null)
    writeStorage(STORAGE_KEYS.podId, null)
    writeStorage(STORAGE_KEYS.podName, null)
    writeStorage(STORAGE_KEYS.sprintId, null)
  }, [])

  const value = useMemo<AppContextValue>(
    () => ({
      selectedTeamId,
      selectedTeamName,
      selectedPodId,
      selectedPodName,
      selectedSprintId,
      hasTeam: selectedTeamId !== null,
      hasPod: selectedPodId !== null,
      setTeam,
      syncTeam,
      setPod,
      setSprintId,
      clearPod,
      clearAll,
    }),
    [
      selectedTeamId,
      selectedTeamName,
      selectedPodId,
      selectedPodName,
      selectedSprintId,
      setTeam,
      syncTeam,
      setPod,
      setSprintId,
      clearPod,
      clearAll,
    ],
  )

  return <AppContext.Provider value={value}>{children}</AppContext.Provider>
}

export function useAppContext(): AppContextValue {
  const context = useContext(AppContext)
  if (!context) {
    throw new Error('useAppContext must be used within AppProvider')
  }
  return context
}
