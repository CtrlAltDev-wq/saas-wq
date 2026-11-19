import { http } from './client'
import type { LoginPayload, LoginResult, AuthUser } from './types'

export const authApi = {
  login(payload: LoginPayload): Promise<LoginResult> {
    return http.post<LoginResult>('/auth/login', payload)
  },

  logout(): Promise<void> {
    return http.post<void>('/auth/logout', undefined, { silent: true })
  },

  me(): Promise<AuthUser> {
    return http.get<AuthUser>('/auth/me')
  }
}
