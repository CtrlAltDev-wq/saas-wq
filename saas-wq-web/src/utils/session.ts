import type { AuthUser } from '@/api/types'

const TOKEN_KEY = 'saas-wq.console.token'

const USER_KEY = 'saas-wq.console.user'

/** Reads the persisted JWT, returning an empty string when the visitor is anonymous. */
export function readToken(): string {
  return localStorage.getItem(TOKEN_KEY) ?? ''
}

/** Reads the persisted profile snapshot used to restore the session after a reload. */
export function readUser(): AuthUser | null {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) {
    return null
  }
  try {
    return JSON.parse(raw) as AuthUser
  } catch {
    return null
  }
}

/** Persists the token and the profile together. */
export function writeSession(token: string, user: AuthUser): void {
  localStorage.setItem(TOKEN_KEY, token)
  localStorage.setItem(USER_KEY, JSON.stringify(user))
}

/** Refreshes the profile snapshot without touching the token. */
export function writeUser(user: AuthUser): void {
  localStorage.setItem(USER_KEY, JSON.stringify(user))
}

/** Removes every trace of the current session. */
export function clearSession(): void {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}
