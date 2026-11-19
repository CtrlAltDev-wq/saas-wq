import { http } from './client'
import type { CheckoutPayload, CheckoutSession, Order, PageResult } from './types'

export const orderApi = {
  page(page: number, size: number): Promise<PageResult<Order>> {
    return http.get<PageResult<Order>>('/orders', { params: { page, size } })
  },

  getById(id: number): Promise<Order> {
    return http.get<Order>(`/orders/${id}`)
  },

  checkout(payload: CheckoutPayload): Promise<CheckoutSession> {
    return http.post<CheckoutSession>('/orders/checkout', payload)
  }
}
