<script setup lang="ts">
import { nextTick, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { userApi } from '@/api/user'
import type { SysUser } from '@/api/types'
import { useAuthStore } from '@/stores/auth'
import { accountStatusTag, formatDate } from '@/utils/format'

type TenantRole = 'TENANT_ADMIN' | 'MEMBER'

interface UserForm {
  username: string
  password: string
  nickname: string
  email: string
  role: TenantRole
  status: 'ACTIVE' | 'DISABLED'
}

const auth = useAuthStore()

const loading = ref(false)

const rows = ref<SysUser[]>([])

const total = ref(0)

const page = ref(1)

const size = ref(10)

const keyword = ref('')

const savingId = ref<number | null>(null)

const dialogVisible = ref(false)

const dialogMode = ref<'create' | 'edit'>('create')

const editingId = ref<number | null>(null)

const submitting = ref(false)

const formRef = ref<FormInstance>()

const form = reactive<UserForm>({
  username: '',
  password: '',
  nickname: '',
  email: '',
  role: 'MEMBER',
  status: 'ACTIVE'
})

const rules: FormRules<UserForm> = {
  username: [
    { required: true, message: 'Username is required', trigger: 'blur' },
    {
      pattern: /^[a-zA-Z0-9._-]{3,64}$/,
      message: 'Use 3-64 letters, digits, dot, underscore or hyphen',
      trigger: 'blur'
    }
  ],
  password: [
    { required: true, message: 'Password is required', trigger: 'blur' },
    { min: 8, max: 64, message: 'Password must be 8-64 characters', trigger: 'blur' }
  ],
  email: [{ type: 'email', message: 'Email is invalid', trigger: 'blur' }],
  role: [{ required: true, message: 'Role is required', trigger: 'change' }]
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const result = await userApi.page(page.value, size.value, keyword.value.trim())
    rows.value = result.records
    total.value = result.total
  } finally {
    loading.value = false
  }
}

function search(): void {
  keyword.value = keyword.value.trim()
  if (page.value === 1) {
    void load()
    return
  }
  page.value = 1
}

function resetFilters(): void {
  keyword.value = ''
  if (page.value === 1) {
    void load()
    return
  }
  page.value = 1
}

function openCreate(): void {
  dialogMode.value = 'create'
  editingId.value = null
  form.username = ''
  form.password = ''
  form.nickname = ''
  form.email = ''
  form.role = 'MEMBER'
  form.status = 'ACTIVE'
  dialogVisible.value = true
  void nextTick(() => formRef.value?.clearValidate())
}

function openEdit(row: SysUser): void {
  dialogMode.value = 'edit'
  editingId.value = row.id
  form.username = row.username
  form.password = ''
  form.nickname = row.nickname ?? ''
  form.email = row.email ?? ''
  form.role = row.role
  form.status = row.status
  dialogVisible.value = true
  void nextTick(() => formRef.value?.clearValidate())
}

async function submit(): Promise<void> {
  if (!formRef.value) {
    return
  }
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  submitting.value = true
  try {
    if (dialogMode.value === 'create') {
      await userApi.create({
        username: form.username.trim(),
        password: form.password,
        nickname: form.nickname.trim() || undefined,
        email: form.email.trim() || undefined,
        role: form.role
      })
      ElMessage.success('User created')
    } else if (editingId.value !== null) {
      await userApi.update(editingId.value, {
        nickname: form.nickname.trim() || undefined,
        email: form.email.trim() || undefined,
        role: form.role,
        status: form.status
      })
      ElMessage.success('User updated')
      if (editingId.value === auth.user?.id) {
        await auth.fetchProfile()
      }
    }
    dialogVisible.value = false
    await load()
  } catch {
    // The response interceptor already surfaced the failure.
  } finally {
    submitting.value = false
  }
}

async function setStatus(row: SysUser, value: string | number | boolean): Promise<void> {
  const enabled = value === true
  savingId.value = row.id
  try {
    const updated = await userApi.update(row.id, {
      nickname: row.nickname ?? undefined,
      email: row.email ?? undefined,
      role: row.role,
      status: enabled ? 'ACTIVE' : 'DISABLED'
    })
    row.status = updated.status
    ElMessage.success(enabled ? 'User enabled' : 'User disabled')
  } catch {
    // The switch reflects row.status, so a failure leaves it untouched.
  } finally {
    savingId.value = null
  }
}

async function remove(row: SysUser): Promise<void> {
  try {
    await ElMessageBox.confirm(`Delete user "${row.username}"? This cannot be undone.`, 'Delete user', {
      type: 'warning',
      confirmButtonText: 'Delete',
      cancelButtonText: 'Cancel'
    })
  } catch {
    return
  }
  try {
    await userApi.remove(row.id)
    ElMessage.success('User deleted')
    if (rows.value.length === 1 && page.value > 1) {
      page.value -= 1
      return
    }
    await load()
  } catch {
    // The response interceptor already surfaced the failure.
  }
}

watch([page, size], () => {
  void load()
})

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">Users</h1>
        <p class="page-subtitle">
          Accounts of the current tenant. Roles decide which console pages a user can reach.
        </p>
      </div>
      <el-button type="primary" @click="openCreate">
        <el-icon><Plus /></el-icon>
        <span>New user</span>
      </el-button>
    </div>

    <section class="panel">
      <div class="panel-header">
        <div class="toolbar">
          <el-input
            v-model="keyword"
            placeholder="Search username or nickname"
            clearable
            class="search"
            @keyup.enter="search"
            @clear="resetFilters"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <el-button @click="search">Search</el-button>
        </div>
        <el-button text :icon="'Refresh'" @click="load">Refresh</el-button>
      </div>

      <el-table v-loading="loading" :data="rows" empty-text="No users in this tenant">
        <el-table-column label="Username" min-width="160">
          <template #default="{ row }">
            <span class="mono">{{ row.username }}</span>
            <el-tag v-if="row.id === auth.user?.id" size="small" effect="plain" class="self-tag">you</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="nickname" label="Nickname" min-width="150">
          <template #default="{ row }">{{ row.nickname || '—' }}</template>
        </el-table-column>
        <el-table-column prop="email" label="Email" min-width="200">
          <template #default="{ row }">{{ row.email || '—' }}</template>
        </el-table-column>
        <el-table-column label="Role" width="150">
          <template #default="{ row }">
            <el-tag :type="row.role === 'TENANT_ADMIN' ? 'primary' : 'info'" size="small" effect="light">
              {{ row.role === 'TENANT_ADMIN' ? 'Tenant admin' : 'Member' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Status" width="160">
          <template #default="{ row }">
            <div class="status-cell">
              <el-switch
                :model-value="row.status === 'ACTIVE'"
                :loading="savingId === row.id"
                :disabled="row.id === auth.user?.id"
                @change="(value: string | number | boolean) => setStatus(row, value)"
              />
              <el-tag :type="accountStatusTag(row.status)" size="small" effect="light">
                {{ row.status }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="Created" width="140">
          <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="Actions" width="140" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="openEdit(row)">Edit</el-button>
            <el-button text type="danger" :disabled="row.id === auth.user?.id" @click="remove(row)">
              Delete
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="table-footer">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="page = 1"
        />
      </div>
    </section>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? 'New user' : 'Edit user'"
      width="500px"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="form-grid">
          <el-form-item label="Username" prop="username">
            <el-input v-model="form.username" :disabled="dialogMode === 'edit'" placeholder="jane.doe" />
          </el-form-item>
          <el-form-item v-if="dialogMode === 'create'" label="Password" prop="password">
            <el-input v-model="form.password" type="password" show-password placeholder="At least 8 characters" />
          </el-form-item>
          <el-form-item label="Nickname" prop="nickname">
            <el-input v-model="form.nickname" placeholder="Jane Doe" />
          </el-form-item>
          <el-form-item label="Email" prop="email">
            <el-input v-model="form.email" placeholder="jane@acme.dev" />
          </el-form-item>
        </div>
        <el-form-item label="Role" prop="role">
          <el-radio-group v-model="form.role">
            <el-radio-button value="TENANT_ADMIN">Tenant admin</el-radio-button>
            <el-radio-button value="MEMBER">Member</el-radio-button>
          </el-radio-group>
          <span class="field-hint">
            Tenant admins manage customers, billing and users. Members have read access.
          </span>
        </el-form-item>
        <el-form-item v-if="dialogMode === 'edit'" label="Status" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio-button value="ACTIVE">Active</el-radio-button>
            <el-radio-button value="DISABLED">Disabled</el-radio-button>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">Cancel</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">
          {{ dialogMode === 'create' ? 'Create user' : 'Save changes' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.search {
  width: 280px;
}

.status-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.self-tag {
  margin-left: 6px;
  border-color: #d7e2fb;
  color: #2f6bf3;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

.field-hint {
  margin-top: 6px;
  font-size: 11px;
  color: #98a2b3;
  line-height: 1.4;
}

@media (max-width: 640px) {
  .search {
    width: 100%;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
