import { apiClient } from './apiClient'

export interface JqlFieldReferenceDto {
  value: string
  displayName: string
  cfid: string | null
  operators: string[]
}

export interface JqlFunctionReferenceDto {
  value: string
  displayName: string
}

export interface JqlReferenceDataDto {
  jqlReservedWords: string[]
  visibleFieldNames: JqlFieldReferenceDto[]
  visibleFunctionNames: JqlFunctionReferenceDto[]
}

export interface JqlSuggestionItemDto {
  value: string
  displayName: string
}

export interface JqlSuggestionsDto {
  results: JqlSuggestionItemDto[]
}

export function getJqlReferenceData(podId: string): Promise<JqlReferenceDataDto> {
  return apiClient.get<JqlReferenceDataDto>(`/pods/${podId}/jql/reference`)
}

export function getJqlSuggestions(
  podId: string,
  fieldName: string,
  fieldValue?: string | null,
): Promise<JqlSuggestionsDto> {
  const params = new URLSearchParams({ fieldName })
  if (fieldValue != null && fieldValue.trim()) {
    params.set('fieldValue', fieldValue.trim())
  }
  return apiClient.get<JqlSuggestionsDto>(`/pods/${podId}/jql/suggestions?${params.toString()}`)
}
