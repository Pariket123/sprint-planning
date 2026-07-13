import { useCallback, useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { listReleases } from '../api'
import type { ReleaseResponse } from '../api/types'
import { PageHeader, PageLoadingState } from '../components/common'
import { ReleaseIssuesTab } from '../components/releases/ReleaseIssuesTab'
import { ReleasesTab } from '../components/releases/ReleasesTab'

type ReleaseView = 'list' | 'issues'

export function ReleasePage() {
  const { podId } = useParams<{ podId: string }>()
  const [view, setView] = useState<ReleaseView>('list')
  const [releases, setReleases] = useState<ReleaseResponse[]>([])
  const [selectedReleaseId, setSelectedReleaseId] = useState<string | null>(null)
  const [bootstrapping, setBootstrapping] = useState(true)

  const bootstrapReleases = useCallback(async () => {
    if (!podId) {
      setBootstrapping(false)
      return
    }

    setBootstrapping(true)
    try {
      const data = await listReleases(podId)
      setReleases(data)
    } catch {
      setReleases([])
    } finally {
      setBootstrapping(false)
    }
  }, [podId])

  useEffect(() => {
    void bootstrapReleases()
  }, [bootstrapReleases])

  useEffect(() => {
    if (view !== 'issues' || !selectedReleaseId) {
      return
    }
    if (!releases.some((release) => release.id === selectedReleaseId)) {
      setView('list')
      setSelectedReleaseId(null)
    }
  }, [view, selectedReleaseId, releases])

  const handleReleasesChange = useCallback((nextReleases: ReleaseResponse[]) => {
    setReleases(nextReleases)
  }, [])

  const handleViewReleaseIssues = useCallback((releaseId: string) => {
    setSelectedReleaseId(releaseId)
    setView('issues')
  }, [])

  const handleBackToReleases = useCallback(() => {
    setView('list')
    setSelectedReleaseId(null)
  }, [])

  const selectedRelease =
    selectedReleaseId !== null
      ? releases.find((release) => release.id === selectedReleaseId) ?? null
      : null

  if (!podId) {
    return null
  }

  if (bootstrapping) {
    return (
      <>
        <PageHeader
          title="Create/View Release"
          description="Manage release configurations and search release issues."
        />
        <PageLoadingState message="Loading releases..." />
      </>
    )
  }

  return (
    <div className="space-y-6">
      <PageHeader
        title="Create/View Release"
        description={
          view === 'list'
            ? 'Manage team release configurations. Releases are shared across all pods in the team.'
            : 'Search issues scoped to the selected release.'
        }
      />

      {view === 'list' ? (
        <ReleasesTab
          podId={podId}
          initialReleases={releases}
          onReleasesChange={handleReleasesChange}
          onViewIssues={handleViewReleaseIssues}
        />
      ) : selectedRelease ? (
        <ReleaseIssuesTab
          podId={podId}
          release={selectedRelease}
          onBack={handleBackToReleases}
        />
      ) : (
        <ReleasesTab
          podId={podId}
          initialReleases={releases}
          onReleasesChange={handleReleasesChange}
          onViewIssues={handleViewReleaseIssues}
        />
      )}
    </div>
  )
}
