import { http } from './client'
import type { PageResult, Tenant, TenantCreatePayload, TenantUpdatePayload } from './types'

export const tenantApi = {
  page(page: number, size: number, keyword?: string): Promise<PageResult<Tenant>> {
    return http.get<PageResult<Tenant>>('/tenants', {
      params: { page, size, keyword: keyword || undefined }
    })
  },

  create(payload: TenantCreatePayload): Promise<Tenant> {
    return http.post<Tenant>('/tenants', payload)
  },

  update(id: number, payload: TenantUpdatePayload): Promise<Tenant> {
    return http.put<Tenant>(`/tenants/${id}`, payload)
  },

  remove(id: number): Promise<void> {
    return http.delete<void>(`/tenants/${id}`)
  }
}
