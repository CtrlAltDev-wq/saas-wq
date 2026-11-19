import { http } from './client'
import type { PageResult, SysUser, UserCreatePayload, UserUpdatePayload } from './types'

export const userApi = {
  page(page: number, size: number, keyword?: string): Promise<PageResult<SysUser>> {
    return http.get<PageResult<SysUser>>('/users', {
      params: { page, size, keyword: keyword || undefined }
    })
  },

  create(payload: UserCreatePayload): Promise<SysUser> {
    return http.post<SysUser>('/users', payload)
  },

  update(id: number, payload: UserUpdatePayload): Promise<SysUser> {
    return http.put<SysUser>(`/users/${id}`, payload)
  },

  remove(id: number): Promise<void> {
    return http.delete<void>(`/users/${id}`)
  }
}
