import axios, { type AxiosError } from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { useTrafficStore } from '@/stores/traffic'
import { pinia } from '@/stores/pinia'
import { clearSession, readToken } from '@/utils/session'
import type { ApiResult } from './types'

declare module 'axios' {
  export interface AxiosRequestConfig {
    /** Suppresses the toast for expected failures, for example a missing subscription. */
    silent?: boolean
  }
}

/** Error raised for every non success HTTP response, carrying the business code. */
export class ApiError extends Error {
  readonly status?: number

  readonly code?: number

  constructor(message: string, options?: { status?: number; code?: number }) {
    super(message)
    this.name = 'ApiError'
    this.status = options?.status
    this.code = options?.code
  }
}

export interface RequestOptions {
  params?: Record<string, unknown>
  silent?: boolean
}

const instance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api',
  timeout: 20000,
  headers: { 'Content-Type': 'application/json' }
})

instance.interceptors.request.use((config) => {
  const token = readToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  useTrafficStore(pinia).track()
  return config
})

instance.interceptors.response.use(
  (response) => {
    const body = response.data as ApiResult<unknown> | undefined
    if (body && typeof body.code === 'number') {
      if (body.code === 200) {
        return body.data
      }
      return Promise.reject(new ApiError(body.message, { status: response.status, code: body.code }))
    }
    return response.data
  },
  (error: AxiosError<Partial<ApiResult<unknown>> | undefined>) => {
    const status = error.response?.status
    const body = error.response?.data
    const config = error.config
    const silent = config?.silent === true
    const isLoginRequest = (config?.url ?? '').endsWith('/auth/login')

    if (status === 401 && !isLoginRequest) {
      clearSession()
      const current = router.currentRoute.value
      if (current.name !== 'login') {
        void router.replace({ name: 'login', query: { redirect: current.fullPath } })
      }
      if (!silent) {
        ElMessage.error('Your session has expired. Please sign in again.')
      }
      return Promise.reject(new ApiError('Unauthorized', { status, code: body?.code }))
    }

    const message = resolveMessage(error, status, body)
    if (!silent) {
      ElMessage.error(message)
    }
    return Promise.reject(new ApiError(message, { status, code: body?.code }))
  }
)

/**
 * Turns a failed request into a message a business user can act on. The backend
 * message wins because it already describes the concrete business rule.
 */
function resolveMessage(
  error: AxiosError<Partial<ApiResult<unknown>> | undefined>,
  status: number | undefined,
  body: Partial<ApiResult<unknown>> | undefined
): string {
  if (body && typeof body.message === 'string' && body.message.length > 0) {
    return body.message
  }
  if (status === 403) {
    return 'You do not have permission to perform this action.'
  }
  if (status === 500) {
    return 'The server encountered an error. Please try again later.'
  }
  if (status === 502 || status === 503) {
    return 'The service is temporarily unavailable. Please try again later.'
  }
  if (status === 400) {
    return 'The request could not be processed. Please review the submitted values.'
  }
  if (!status) {
    return 'Unable to reach the server. Check your connection and try again.'
  }
  return error.message || 'Request failed'
}

/**
 * Thin typed facade over Axios. Response interceptors already unwrap the unified
 * `Result` envelope, so every helper resolves with the payload only.
 */
export const http = {
  async get<T>(url: string, options?: RequestOptions): Promise<T> {
    const payload = await instance.get(url, { params: options?.params, silent: options?.silent })
    return payload as T
  },
  async post<T>(url: string, data?: unknown, options?: RequestOptions): Promise<T> {
    const payload = await instance.post(url, data, { silent: options?.silent })
    return payload as T
  },
  async put<T>(url: string, data?: unknown, options?: RequestOptions): Promise<T> {
    const payload = await instance.put(url, data, { silent: options?.silent })
    return payload as T
  },
  async delete<T>(url: string, options?: RequestOptions): Promise<T> {
    const payload = await instance.delete(url, { silent: options?.silent })
    return payload as T
  }
}
