import type { JqlFieldReferenceDto, JqlReferenceDataDto } from '../api/jqlApi'

export type JqlSuggestionKind = 'field' | 'operator' | 'value' | 'keyword' | 'function'

export interface JqlSuggestionOption {
  kind: JqlSuggestionKind
  value: string
  label: string
}

export interface JqlClauseContext {
  kind: JqlSuggestionKind
  field: JqlFieldReferenceDto | null
  partial: string
  replaceStart: number
  replaceEnd: number
}

const OPERATOR_PATTERN =
  /^(=|!=|~|!~|>|<|>=|<=|in|not in|is|is not|was|was not|changed)$/i

const CLAUSE_SPLIT = /\s+(?:AND|OR)\s+/i

function stripHtml(value: string): string {
  return value.replace(/<[^>]*>/g, '')
}

function lastClause(textBeforeCursor: string): string {
  const parts = textBeforeCursor.split(CLAUSE_SPLIT)
  return parts[parts.length - 1] ?? ''
}

function findField(
  token: string,
  reference: JqlReferenceDataDto | null,
): JqlFieldReferenceDto | null {
  if (!reference || !token) {
    return null
  }

  const normalized = token.toLowerCase()
  const unquoted = token.replace(/^"(.*)"$/, '$1').toLowerCase()

  return (
    reference.visibleFieldNames.find((field) => {
      const candidates = [field.value, field.displayName, field.cfid]
        .filter(Boolean)
        .map((candidate) => candidate!.toLowerCase())
      return candidates.some(
        (candidate) => candidate === normalized || candidate === unquoted,
      )
    }) ?? null
  )
}

function tokenBounds(text: string, cursor: number): { start: number; end: number; token: string } {
  let start = cursor
  while (start > 0) {
    const char = text[start - 1]
    if (/[\s(),]/.test(char)) {
      break
    }
    start -= 1
  }

  let end = cursor
  while (end < text.length) {
    const char = text[end]
    if (/[\s(),]/.test(char)) {
      break
    }
    end += 1
  }

  return { start, end, token: text.slice(start, end) }
}

export function resolveJqlClauseContext(
  query: string,
  cursor: number,
  reference: JqlReferenceDataDto | null,
): JqlClauseContext {
  const safeCursor = Math.max(0, Math.min(cursor, query.length))
  const textBefore = query.slice(0, safeCursor)
  const clause = lastClause(textBefore).trimStart()
  const clauseOffset = textBefore.length - clause.length
  const { start, end, token } = tokenBounds(query, safeCursor)
  const partial = token

  if (!clause) {
    return {
      kind: 'field',
      field: null,
      partial,
      replaceStart: start,
      replaceEnd: end,
    }
  }

  const operatorMatch = clause.match(
    /^("(?:[^"\\]|\\.)+"|[^\s]+)\s+(=|!=|~|!~|>|<|>=|<=|IN|NOT IN|IS|IS NOT|WAS|WAS NOT|CHANGED)\s*(.*)$/i,
  )

  if (operatorMatch) {
    const fieldToken = operatorMatch[1]
    const valuePart = operatorMatch[3] ?? ''
    const field = findField(fieldToken, reference)
    const valueCursorInClause = clause.length - valuePart.length
    const valueTokenStart = clauseOffset + valueCursorInClause + (valuePart.length - partial.length)

    return {
      kind: 'value',
      field,
      partial,
      replaceStart: Math.max(clauseOffset, valueTokenStart),
      replaceEnd: end,
    }
  }

  const fieldOnlyMatch = clause.match(/^("(?:[^"\\]|\\.)+"|[^\s]+)\s+(.*)$/i)
  if (fieldOnlyMatch) {
    const fieldToken = fieldOnlyMatch[1]
    const afterField = fieldOnlyMatch[2] ?? ''
    const field = findField(fieldToken, reference)

    if (field && afterField.length > 0 && !OPERATOR_PATTERN.test(afterField.trim())) {
      return {
        kind: 'operator',
        field,
        partial: afterField.trim(),
        replaceStart: start,
        replaceEnd: end,
      }
    }

    if (field && afterField.trim().length === 0) {
      return {
        kind: 'operator',
        field,
        partial: '',
        replaceStart: safeCursor,
        replaceEnd: safeCursor,
      }
    }
  }

  if (/^(AND|OR)$/i.test(partial) || clause.trim().length === 0) {
    return {
      kind: 'field',
      field: null,
      partial,
      replaceStart: start,
      replaceEnd: end,
    }
  }

  return {
    kind: 'field',
    field: findField(partial, reference),
    partial,
    replaceStart: start,
    replaceEnd: end,
  }
}

function matchesPrefix(value: string, partial: string): boolean {
  if (!partial) {
    return true
  }
  return value.toLowerCase().includes(partial.toLowerCase())
}

export function buildLocalJqlSuggestions(
  context: JqlClauseContext,
  reference: JqlReferenceDataDto | null,
): JqlSuggestionOption[] {
  if (!reference) {
    return []
  }

  const partial = context.partial

  if (context.kind === 'operator' && context.field) {
    return context.field.operators
      .filter((operator) => matchesPrefix(operator, partial))
      .map((operator) => ({
        kind: 'operator' as const,
        value: operator,
        label: operator,
      }))
  }

  if (context.kind === 'field') {
    const fieldSuggestions = reference.visibleFieldNames
      .filter(
        (field) =>
          matchesPrefix(field.value, partial)
          || matchesPrefix(field.displayName, partial)
          || (field.cfid != null && matchesPrefix(field.cfid, partial)),
      )
      .slice(0, 15)
      .map((field) => ({
        kind: 'field' as const,
        value: field.value.includes(' ') ? `"${field.displayName}"` : field.value,
        label: stripHtml(field.displayName),
      }))

    const keywordSuggestions = reference.jqlReservedWords
      .filter((word) => matchesPrefix(word, partial))
      .slice(0, 5)
      .map((word) => ({
        kind: 'keyword' as const,
        value: word.toUpperCase(),
        label: word.toUpperCase(),
      }))

    const functionSuggestions = reference.visibleFunctionNames
      .filter(
        (fn) =>
          matchesPrefix(fn.value, partial) || matchesPrefix(fn.displayName, partial),
      )
      .slice(0, 5)
      .map((fn) => ({
        kind: 'function' as const,
        value: fn.value,
        label: stripHtml(fn.displayName),
      }))

    return [...fieldSuggestions, ...keywordSuggestions, ...functionSuggestions].slice(0, 20)
  }

  return []
}

export function formatJqlSuggestionValue(value: string, kind: JqlSuggestionKind): string {
  if (kind === 'operator') {
    return value
  }
  if (kind === 'keyword' || kind === 'function') {
    return value
  }
  if (/\s/.test(value)) {
    return `"${value.replace(/"/g, '\\"')}"`
  }
  return value
}

export function applyJqlSuggestion(
  query: string,
  context: JqlClauseContext,
  suggestion: JqlSuggestionOption,
): { nextValue: string; nextCursor: number } {
  const insertValue = formatJqlSuggestionValue(suggestion.value, suggestion.kind)
  const needsSpaceAfter =
    suggestion.kind === 'field' || suggestion.kind === 'operator' || suggestion.kind === 'keyword'

  const before = query.slice(0, context.replaceStart)
  const after = query.slice(context.replaceEnd)
  const spacer = needsSpaceAfter ? ' ' : ''
  const nextValue = `${before}${insertValue}${spacer}${after}`
  const nextCursor = before.length + insertValue.length + spacer.length

  return { nextValue, nextCursor }
}
