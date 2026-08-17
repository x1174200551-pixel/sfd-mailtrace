import { TOKEN_KEY } from '../../constants/storage'
import type { BasicResult } from '../../types/common'
import { ApiError } from './error-handler'
import { createTraceId } from './trace'

export type RequestOptions = RequestInit & {
  skipAuth?: boolean
}

export function readStoredToken() {
  return localStorage.getItem(TOKEN_KEY) || sessionStorage.getItem(TOKEN_KEY) || ''
}

export function authHeaders(token: string) {
  return { Authorization: `Bearer ${token}` }
}

export async function requestApi<T>(url: string, options: RequestOptions = {}) {
  const { skipAuth, ...fetchOptions } = options
  const headers = new Headers(fetchOptions.headers)
  const isFormData = fetchOptions.body instanceof FormData
  const traceId = headers.get('X-Trace-Id') || createTraceId()

  if (!headers.has('Content-Type') && !isFormData) {
    headers.set('Content-Type', 'application/json')
  }
  if (!headers.has('X-Trace-Id')) {
    headers.set('X-Trace-Id', traceId)
  }
  if (!skipAuth && !headers.has('Authorization') && !url.includes('/auth/login')) {
    const storedToken = readStoredToken()
    if (storedToken) {
      headers.set('Authorization', `Bearer ${storedToken}`)
    }
  }

  let response: Response
  try {
    response = await fetch(url, {
      ...fetchOptions,
      headers: Object.fromEntries(headers.entries()),
    })
  } catch (error) {
    throw new ApiError(error instanceof Error ? error.message : '网络请求失败', 0, undefined, traceId)
  }

  const text = await response.text().catch(() => '')
  const body = parseResult<T>(text, response, traceId)

  if (!response.ok || body.code !== 0) {
    throw new ApiError(body.message || `请求失败：${response.status}`, response.status, body.code, traceId)
  }

  return body.data
}

function parseResult<T>(text: string, response: Response, traceId: string): BasicResult<T> {
  if (!text) {
    return {
      code: response.ok ? 0 : response.status,
      message: response.ok ? 'OK' : `请求失败：${response.status}`,
      data: undefined as T,
    }
  }
  try {
    return JSON.parse(text) as BasicResult<T>
  } catch {
    throw new ApiError(`响应解析失败：${response.status}`, response.status, undefined, traceId)
  }
}
