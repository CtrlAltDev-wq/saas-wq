/**
 * Shared API contracts. Field names mirror the backend view objects so the
 * payloads can be consumed without an intermediate mapping layer.
 */

export type Role = 'SUPER_ADMIN' | 'TENANT_ADMIN' | 'MEMBER'

export type TenantStatus = 'ACTIVE' | 'DISABLED'

export type CustomerStatus = 'ACTIVE' | 'INACTIVE'

export type PaymentStatus = 'PENDING' | 'PAID' | 'FAILED'

export type SubscriptionStatus = 'INCOMPLETE' | 'ACTIVE' | 'PAST_DUE' | 'CANCELED'

/** Unified response envelope returned by every backend endpoint. */
export interface ApiResult<T> {
  code: number
  message: string
  data: T
  timestamp: number
}

/** Paging payload used by every list endpoint. */
export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
  pages: number
}

export interface AuthUser {
  id: number
  tenantId: number
  username: string
  nickname: string | null
  email: string | null
  role: Role
  status: string
  createdAt: string
}

export interface LoginResult {
  accessToken: string
  tokenType: string
  expiresIn: number
  user: AuthUser
}

export interface Tenant {
  id: number
  code: string
  name: string
  contactEmail: string | null
  status: TenantStatus
  planCode: string | null
  planExpiresAt: string | null
  createdAt: string
}

export interface Customer {
  id: number
  name: string
  company: string | null
  email: string | null
  phone: string | null
  stripeCustomerId: string | null
  status: CustomerStatus
  remark: string | null
  createdAt: string
}

export interface Order {
  id: number
  orderNo: string
  customerId: number
  planCode: string
  amountCents: number
  currency: string
  status: PaymentStatus
  stripeCheckoutSessionId: string | null
  stripeSubscriptionId: string | null
  paidAt: string | null
  createdAt: string
}

export interface Subscription {
  id: number
  planCode: string
  status: SubscriptionStatus
  stripeSubscriptionId: string | null
  currentPeriodStart: string | null
  currentPeriodEnd: string | null
  createdAt: string
}

export interface CheckoutSession {
  orderId: number
  orderNo: string
  sessionId: string
  checkoutUrl: string
  amountCents: number
  currency: string
}

export interface SysUser {
  id: number
  tenantId: number
  username: string
  nickname: string | null
  email: string | null
  role: Exclude<Role, 'SUPER_ADMIN'>
  status: 'ACTIVE' | 'DISABLED'
  createdAt: string
}

export interface LoginPayload {
  tenantCode: string
  username: string
  password: string
}

export interface TenantCreatePayload {
  code: string
  name: string
  contactEmail?: string
}

export interface TenantUpdatePayload {
  name: string
  contactEmail?: string
  status?: TenantStatus
}

export interface CustomerPayload {
  name: string
  company?: string
  email?: string
  phone?: string
  status?: CustomerStatus
  remark?: string
}

export interface UserCreatePayload {
  username: string
  password: string
  nickname?: string
  email?: string
  role: Exclude<Role, 'SUPER_ADMIN'>
}

export interface UserUpdatePayload {
  nickname?: string
  email?: string
  role: Exclude<Role, 'SUPER_ADMIN'>
  status: 'ACTIVE' | 'DISABLED'
}

export interface CheckoutPayload {
  customerId: number
  planCode: string
}

export interface RoleDefinition {
  code: Role
  name: string
  description: string | null
  builtIn: boolean
  permissions: string[]
}

export interface PermissionDefinition {
  code: string
  name: string
  resource: string
  action: string
  description: string | null
}

export interface AuditLog {
  id: number
  tenantId: number
  userId: number | null
  username: string | null
  action: string
  resource: string
  httpMethod: string
  uri: string
  requestArgs: string | null
  success: boolean
  errorMessage: string | null
  durationMs: number | null
  ipAddress: string | null
  createdAt: string
}

export interface AuditLogQuery {
  action?: string
  username?: string
  success?: boolean
}
