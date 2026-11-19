import { http } from './client'
import type { Customer, CustomerPayload, PageResult } from './types'

export const customerApi = {
  page(page: number, size: number, keyword?: string): Promise<PageResult<Customer>> {
    return http.get<PageResult<Customer>>('/customers', {
      params: { page, size, keyword: keyword || undefined }
    })
  },

  create(payload: CustomerPayload): Promise<Customer> {
    return http.post<Customer>('/customers', payload)
  },

  update(id: number, payload: CustomerPayload): Promise<Customer> {
    return http.put<Customer>(`/customers/${id}`, payload)
  },

  remove(id: number): Promise<void> {
    return http.delete<void>(`/customers/${id}`)
  }
}
