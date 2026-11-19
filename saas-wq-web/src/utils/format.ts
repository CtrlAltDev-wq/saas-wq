/** Number formatting, date formatting and status helpers shared by every table. */

export type TagType = 'primary' | 'success' | 'warning' | 'info' | 'danger'

/** Formats an amount stored in the smallest currency unit, for example 9900 -> $99.00. */
export function formatMoney(cents: number | null | undefined, currency = 'usd'): string {
  const amount = (cents ?? 0) / 100
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: (currency || 'usd').toUpperCase(),
    maximumFractionDigits: 2
  }).format(amount)
}

/** Formats an ISO-8601 local date time produced by the backend. */
export function formatDateTime(value: string | null | undefined): string {
  if (!value) {
    return '—'
  }
  const normalised = value.includes('T') ? value : value.replace(' ', 'T')
  const date = new Date(normalised)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return new Intl.DateTimeFormat('en-GB', { dateStyle: 'medium', timeStyle: 'short' }).format(date)
}

/** Formats an ISO-8601 value as a calendar date only. */
export function formatDate(value: string | null | undefined): string {
  if (!value) {
    return '—'
  }
  const normalised = value.includes('T') ? value : value.replace(' ', 'T')
  const date = new Date(normalised)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return new Intl.DateTimeFormat('en-GB', { dateStyle: 'medium' }).format(date)
}

/** Turns STARTER into Starter for display purposes. */
export function planLabel(code: string | null | undefined): string {
  if (!code) {
    return '—'
  }
  return code.charAt(0).toUpperCase() + code.slice(1).toLowerCase()
}

/** Maps an order status to an Element Plus tag type. */
export function orderStatusTag(status: string): TagType {
  if (status === 'PAID') {
    return 'success'
  }
  if (status === 'PENDING') {
    return 'warning'
  }
  if (status === 'FAILED') {
    return 'danger'
  }
  return 'info'
}

/** Maps a subscription status to an Element Plus tag type. */
export function subscriptionStatusTag(status: string): TagType {
  if (status === 'ACTIVE') {
    return 'success'
  }
  if (status === 'PAST_DUE') {
    return 'warning'
  }
  if (status === 'CANCELED') {
    return 'danger'
  }
  return 'info'
}

/** Maps an account status to an Element Plus tag type. */
export function accountStatusTag(status: string): TagType {
  return status === 'ACTIVE' ? 'success' : 'info'
}
