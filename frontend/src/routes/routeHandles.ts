export interface RouteHandle {
  section?: string
}

declare module 'react-router' {
  interface RouteHandle {
    section?: string
  }
}

export function getRouteSection(matches: Array<{ handle?: unknown }>): string | undefined {
  for (let index = matches.length - 1; index >= 0; index -= 1) {
    const handle = matches[index].handle as RouteHandle | undefined
    if (handle?.section) {
      return handle.section
    }
  }
  return undefined
}
