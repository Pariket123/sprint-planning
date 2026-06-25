import { ApiError, type ApiResponse } from './types'

const API_BASE_URL = '/api/v1'

type RequestOptions = Omit<RequestInit, 'body'> & {
  body?: unknown
}

async function parseJsonResponse(response: Response): Promise<unknown> {
  const text = await response.text()
  if (!text) {
    return null
  }

  try {
    return JSON.parse(text) as unknown
  } catch {
    throw new ApiError('Received an invalid response from the server.', 'INVALID_JSON', response.status)
  }
}

function unwrapApiResponse<T>(payload: unknown, status: number): T {
  if (!payload || typeof payload !== 'object' || !('success' in payload)) {
    throw new ApiError('Unexpected response format from the server.', 'INVALID_RESPONSE', status)
  }

  const apiResponse = payload as ApiResponse<T>

  if (apiResponse.success) {
    return apiResponse.data as T
  }

  const error = apiResponse.error
  throw new ApiError(
    error?.message ?? 'Request failed.',
    error?.code ?? 'API_ERROR',
    status,
  )
}

async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { body, headers, ...rest } = options

  const init: RequestInit = {
    ...rest,
    headers: {
      Accept: 'application/json',
      ...(body !== undefined ? { 'Content-Type': 'application/json' } : {}),
      ...headers,
    },
    body: body !== undefined ? JSON.stringify(body) : undefined,
  }

  let response: Response

  try {
    response = await fetch(`${API_BASE_URL}${path}`, init)
  } catch {
    throw new ApiError(
      'Unable to reach the server. Check that the backend is running on port 8080.',
      'NETWORK_ERROR',
    )
  }

  const payload = await parseJsonResponse(response)

  if (!response.ok && (!payload || typeof payload !== 'object' || !('success' in payload))) {
    throw new ApiError(
      `Request failed with status ${response.status}.`,
      'HTTP_ERROR',
      response.status,
    )
  }

  return unwrapApiResponse<T>(payload, response.status)
}

export const apiClient = {
  get<T>(path: string, init?: Omit<RequestOptions, 'body' | 'method'>): Promise<T> {
    return request<T>(path, { ...init, method: 'GET' })
  },

  post<T>(path: string, body?: unknown, init?: Omit<RequestOptions, 'body' | 'method'>): Promise<T> {
    return request<T>(path, { ...init, method: 'POST', body })
  },

  put<T>(path: string, body?: unknown, init?: Omit<RequestOptions, 'body' | 'method'>): Promise<T> {
    return request<T>(path, { ...init, method: 'PUT', body })
  },

  delete<T>(path: string, init?: Omit<RequestOptions, 'body' | 'method'>): Promise<T> {
    return request<T>(path, { ...init, method: 'DELETE' })
  },
}
