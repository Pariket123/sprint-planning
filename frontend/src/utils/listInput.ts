export function parseTagInput(value: string): string[] {
  return value
    .split(',')
    .map((item) => item.trim())
    .filter((item) => item.length > 0)
}

export function joinTagInput(values: string[] | null | undefined): string {
  if (!values || values.length === 0) {
    return ''
  }
  return values.join(', ')
}

export function parseNumberListInput(value: string): number[] {
  return value
    .split(',')
    .map((item) => item.trim())
    .filter((item) => item.length > 0)
    .map((item) => Number(item))
    .filter((item) => Number.isFinite(item))
}

export function formatListDisplay(values: string[] | null | undefined): string {
  if (!values || values.length === 0) {
    return '-'
  }
  return values.join(', ')
}

export function formatNumberListDisplay(values: number[] | null | undefined): string {
  if (!values || values.length === 0) {
    return '-'
  }
  return values.join(', ')
}
