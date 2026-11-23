import { http } from './client'
import type { PermissionDefinition, RoleDefinition, Role } from './types'

/** Role catalogue and role permission assignment endpoints. */
export const rbacApi = {
  roles(): Promise<RoleDefinition[]> {
    return http.get<RoleDefinition[]>('/rbac/roles')
  },

  permissions(): Promise<PermissionDefinition[]> {
    return http.get<PermissionDefinition[]>('/rbac/permissions')
  },

  updatePermissions(code: Role, permissionCodes: string[]): Promise<RoleDefinition> {
    return http.put<RoleDefinition>(`/rbac/roles/${code}/permissions`, { permissionCodes })
  }
}
