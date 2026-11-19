import { http } from './client'
import type { Subscription } from './types'

export const subscriptionApi = {
  /**
   * Returns the tenant entitlement. A tenant without a subscription answers 404,
   * which callers usually treat as an empty state rather than an error.
   */
  current(silent = false): Promise<Subscription> {
    return http.get<Subscription>('/subscriptions/current', { silent })
  }
}
