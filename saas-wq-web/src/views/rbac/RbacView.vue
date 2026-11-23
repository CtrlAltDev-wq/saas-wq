<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { rbacApi } from '@/api/rbac'
import type { PermissionDefinition, Role, RoleDefinition } from '@/api/types'
import { useAuthStore } from '@/stores/auth'

interface PermissionGroup {
  resource: string
  items: PermissionDefinition[]
}

const auth = useAuthStore()

const loading = ref(false)

const saving = ref(false)

const roles = ref<RoleDefinition[]>([])

const permissions = ref<PermissionDefinition[]>([])

const dialogVisible = ref(false)

const editingRole = ref<RoleDefinition | null>(null)

const selected = ref<string[]>([])

const canEdit = computed(() => auth.isSuperAdmin)

const groups = computed<PermissionGroup[]>(() => {
  const grouped = new Map<string, PermissionDefinition[]>()
  permissions.value.forEach((permission) => {
    const list = grouped.get(permission.resource) ?? []
    list.push(permission)
    grouped.set(permission.resource, list)
  })
  return Array.from(grouped.entries())
    .sort(([left], [right]) => left.localeCompare(right))
    .map(([resource, items]) => ({
      resource,
      items: [...items].sort((left, right) => left.code.localeCompare(right.code))
    }))
})

function roleLabel(code: Role): string {
  if (code === 'SUPER_ADMIN') {
    return 'Platform super admin'
  }
  if (code === 'TENANT_ADMIN') {
    return 'Tenant admin'
  }
  return 'Member'
}

function roleTagType(code: Role): 'danger' | 'primary' | 'info' {
  if (code === 'SUPER_ADMIN') {
    return 'danger'
  }
  return code === 'TENANT_ADMIN' ? 'primary' : 'info'
}

function previewPermissions(role: RoleDefinition): string[] {
  return role.permissions.slice(0, 3)
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const [roleList, permissionList] = await Promise.all([rbacApi.roles(), rbacApi.permissions()])
    roles.value = roleList
    permissions.value = permissionList
  } finally {
    loading.value = false
  }
}

function openEditor(role: RoleDefinition): void {
  if (!canEdit.value || role.code === 'SUPER_ADMIN') {
    return
  }
  editingRole.value = role
  selected.value = [...role.permissions]
  dialogVisible.value = true
}

function groupSelected(group: PermissionGroup): boolean {
  return group.items.every((item) => selected.value.includes(item.code))
}

function toggleGroup(group: PermissionGroup): void {
  const codes = group.items.map((item) => item.code)
  if (groupSelected(group)) {
    selected.value = selected.value.filter((code) => !codes.includes(code))
    return
  }
  const missing = codes.filter((code) => !selected.value.includes(code))
  selected.value = [...selected.value, ...missing]
}

async function save(): Promise<void> {
  if (!editingRole.value || !canEdit.value) {
    return
  }
  saving.value = true
  try {
    const updated = await rbacApi.updatePermissions(editingRole.value.code, [...selected.value])
    roles.value = roles.value.map((role) => (role.code === updated.code ? updated : role))
    ElMessage.success(`Permissions updated for ${updated.name}. They apply on the next request.`)
    dialogVisible.value = false
  } catch {
    // The response interceptor already surfaced the failure.
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">Roles &amp; permissions</h1>
        <p class="page-subtitle">
          Every endpoint is guarded by a permission code. Changes are cached for 30 minutes but
          invalidated immediately, so they apply on the next request.
        </p>
      </div>
      <el-button text :icon="'Refresh'" @click="load">Refresh</el-button>
    </div>

    <el-alert
      v-if="!canEdit"
      type="info"
      :closable="false"
      show-icon
      title="Read-only view"
      description="Only the platform super administrator can change role permissions."
    />
    <el-alert
      v-else
      type="warning"
      :closable="false"
      show-icon
      title="Administrative change"
      description="Removing a permission takes effect immediately for every user holding the role. SUPER_ADMIN is locked and always keeps the full set."
    />

    <section class="panel">
      <div class="panel-header">
        <h2 class="panel-title">Roles</h2>
        <span class="muted">{{ permissions.length }} permission codes in the catalogue</span>
      </div>

      <el-table v-loading="loading" :data="roles" empty-text="No roles configured">
        <el-table-column label="Role" min-width="200">
          <template #default="{ row }">
            <div class="role-cell">
              <el-tag :type="roleTagType(row.code)" size="small" effect="light">
                {{ roleLabel(row.code) }}
              </el-tag>
              <span class="mono">{{ row.code }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="Description" min-width="260">
          <template #default="{ row }">{{ row.description || '—' }}</template>
        </el-table-column>
        <el-table-column label="Granted permissions" min-width="300">
          <template #default="{ row }">
            <div class="perm-preview">
              <el-tag
                v-for="code in previewPermissions(row)"
                :key="code"
                size="small"
                effect="plain"
                class="perm-chip"
              >
                {{ code }}
              </el-tag>
              <span v-if="row.permissions.length > 3" class="muted">
                +{{ row.permissions.length - 3 }} more
              </span>
              <span v-if="row.permissions.length === 0" class="muted">No permissions</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="Count" width="90" align="center">
          <template #default="{ row }">
            <span class="count">{{ row.permissions.length }}</span>
          </template>
        </el-table-column>
        <el-table-column label="Actions" width="170" fixed="right">
          <template #default="{ row }">
            <el-tooltip
              v-if="row.code === 'SUPER_ADMIN'"
              content="SUPER_ADMIN always keeps every permission."
              placement="top"
            >
              <span>
                <el-button text type="primary" disabled>Edit</el-button>
              </span>
            </el-tooltip>
            <el-button
              v-else
              text
              type="primary"
              :disabled="!canEdit"
              @click="openEditor(row)"
            >
              Edit permissions
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog
      v-model="dialogVisible"
      :title="editingRole ? `Permissions · ${editingRole.name}` : 'Permissions'"
      width="720px"
      top="6vh"
    >
      <p class="dialog-hint">
        Tick the codes granted to
        <span class="mono">{{ editingRole?.code }}</span>
        . A user receives every code of their role on the next request.
      </p>
      <el-checkbox-group v-model="selected" class="perm-groups">
        <div v-for="group in groups" :key="group.resource" class="perm-group">
          <div class="perm-group__head">
            <span class="perm-group__title">{{ group.resource }}</span>
            <el-button link type="primary" size="small" @click="toggleGroup(group)">
              {{ groupSelected(group) ? 'Clear group' : 'Select group' }}
            </el-button>
          </div>
          <div class="perm-group__items">
            <el-checkbox v-for="item in group.items" :key="item.code" :value="item.code">
              <span class="perm-code">{{ item.code }}</span>
              <span class="perm-name">{{ item.name }}</span>
            </el-checkbox>
          </div>
        </div>
      </el-checkbox-group>
      <template #footer>
        <span class="dialog-count">{{ selected.length }} selected</span>
        <el-button @click="dialogVisible = false">Cancel</el-button>
        <el-button type="primary" :loading="saving" @click="save">Save permissions</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.role-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.perm-preview {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.perm-chip {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, 'Liberation Mono', monospace;
  font-size: 11px;
}

.count {
  font-weight: 600;
  color: #344054;
}

.dialog-hint {
  margin: 0 0 14px;
  font-size: 13px;
  color: var(--app-muted);
  line-height: 1.5;
}

.perm-groups {
  display: flex;
  flex-direction: column;
  gap: 4px;
  max-height: 58vh;
  overflow-y: auto;
  padding-right: 6px;
}

.perm-group {
  padding: 12px 14px;
  border: 1px solid var(--app-line);
  border-radius: 10px;
}

.perm-group__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.perm-group__title {
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #667085;
}

.perm-group__items {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 4px 12px;
}

.perm-code {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, 'Liberation Mono', monospace;
  font-size: 12px;
  color: #344054;
}

.perm-name {
  margin-left: 8px;
  font-size: 12px;
  color: #98a2b3;
}

.dialog-count {
  float: left;
  line-height: 32px;
  font-size: 12px;
  color: var(--app-muted);
}

@media (max-width: 720px) {
  .perm-group__items {
    grid-template-columns: 1fr;
  }
}
</style>
