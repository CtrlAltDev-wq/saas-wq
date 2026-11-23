import { http } from './client'
import type { AuditLog, AuditLogQuery, PageResult } from './types'

/** Tenant scoped audit trail queries. */
export const auditApi = {
  page(page: number, size: number, query: AuditLogQuery = {}): Promise<PageResult<AuditLog>> {
    return http.get<PageResult<AuditLog>>('/audit-logs', {
      params: {
        page,
        size,
        action: query.action || undefined,
        username: query.username || undefined,
        success: query.success
      }
    })
  }
}
