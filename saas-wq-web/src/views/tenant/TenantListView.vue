<script setup lang="ts">
import { nextTick, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { tenantApi } from '@/api/tenant'
import type { Tenant } from '@/api/types'
import { accountStatusTag, formatDate, planLabel } from '@/utils/format'

interface TenantForm {
  code: string
  name: string
  contactEmail: string
}

const loading = ref(false)

const rows = ref<Tenant[]>([])

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

const form = reactive<TenantForm>({ code: '', name: '', contactEmail: '' })

const rules: FormRules<TenantForm> = {
  code: [
    { required: true, message: 'Tenant code is required', trigger: 'blur' },
    {
      pattern: /^[a-z0-9][a-z0-9-]{2,63}$/,
      message: 'Use 3-64 lowercase letters, digits or hyphens',
      trigger: 'blur'
    }
  ],
  name: [
    { required: true, message: 'Tenant name is required', trigger: 'blur' },
    { max: 128, message: 'Tenant name must not exceed 128 characters', trigger: 'blur' }
  ],
  contactEmail: [{ type: 'email', message: 'Contact email is invalid', trigger: 'blur' }]
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const result = await tenantApi.page(page.value, size.value, keyword.value.trim())
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
  form.code = ''
  form.name = ''
  form.contactEmail = ''
  dialogVisible.value = true
  void nextTick(() => formRef.value?.clearValidate())
}

function openEdit(row: Tenant): void {
  dialogMode.value = 'edit'
  editingId.value = row.id
  form.code = row.code
  form.name = row.name
  form.contactEmail = row.contactEmail ?? ''
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
    const contactEmail = form.contactEmail.trim() || undefined
    if (dialogMode.value === 'create') {
      await tenantApi.create({ code: form.code.trim(), name: form.name.trim(), contactEmail })
      ElMessage.success('Tenant created')
    } else if (editingId.value !== null) {
      await tenantApi.update(editingId.value, { name: form.name.trim(), contactEmail })
      ElMessage.success('Tenant updated')
    }
    dialogVisible.value = false
    await load()
  } catch {
    // The response interceptor already surfaced the failure.
  } finally {
    submitting.value = false
  }
}

async function setStatus(row: Tenant, value: string | number | boolean): Promise<void> {
  const enabled = value === true
  savingId.value = row.id
  try {
    const updated = await tenantApi.update(row.id, {
      name: row.name,
      contactEmail: row.contactEmail ?? undefined,
      status: enabled ? 'ACTIVE' : 'DISABLED'
    })
    row.status = updated.status
    ElMessage.success(enabled ? 'Tenant enabled' : 'Tenant disabled')
  } catch {
    // The switch reflects row.status, so a failure leaves it untouched.
  } finally {
    savingId.value = null
  }
}

async function remove(row: Tenant): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `Delete tenant "${row.name}"? Tenants that still own users cannot be deleted.`,
      'Delete tenant',
      { type: 'warning', confirmButtonText: 'Delete', cancelButtonText: 'Cancel' }
    )
  } catch {
    return
  }
  try {
    await tenantApi.remove(row.id)
    ElMessage.success('Tenant deleted')
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
        <h1 class="page-title">Tenants</h1>
        <p class="page-subtitle">
          Platform registry of workspaces. Disabling a tenant blocks every sign in attempt.
        </p>
      </div>
      <el-button type="primary" @click="openCreate">
        <el-icon><Plus /></el-icon>
        <span>New tenant</span>
      </el-button>
    </div>

    <section class="panel">
      <div class="panel-header">
        <div class="toolbar">
          <el-input
            v-model="keyword"
            placeholder="Search code or name"
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

      <el-table v-loading="loading" :data="rows" empty-text="No tenants registered">
        <el-table-column label="Code" width="150">
          <template #default="{ row }">
            <span class="mono">{{ row.code }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="Name" min-width="170" />
        <el-table-column label="Contact email" min-width="190">
          <template #default="{ row }">{{ row.contactEmail || '—' }}</template>
        </el-table-column>
        <el-table-column label="Status" width="160">
          <template #default="{ row }">
            <div class="status-cell">
              <el-switch
                :model-value="row.status === 'ACTIVE'"
                :loading="savingId === row.id"
                @change="(value: string | number | boolean) => setStatus(row, value)"
              />
              <el-tag :type="accountStatusTag(row.status)" size="small" effect="light">
                {{ row.status }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="Plan" width="130">
          <template #default="{ row }">
            <el-tag v-if="row.planCode" size="small" effect="plain">{{ planLabel(row.planCode) }}</el-tag>
            <span v-else class="muted">—</span>
          </template>
        </el-table-column>
        <el-table-column label="Plan expiry" width="140">
          <template #default="{ row }">{{ formatDate(row.planExpiresAt) }}</template>
        </el-table-column>
        <el-table-column label="Created" width="150">
          <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="Actions" width="140" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="openEdit(row)">Edit</el-button>
            <el-button text type="danger" @click="remove(row)">Delete</el-button>
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
      :title="dialogMode === 'create' ? 'New tenant' : 'Edit tenant'"
      width="460px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="Tenant code" prop="code">
          <el-input
            v-model="form.code"
            placeholder="acme-inc"
            :disabled="dialogMode === 'edit'"
          />
          <span class="field-hint">Lowercase letters, digits and hyphens. Immutable after creation.</span>
        </el-form-item>
        <el-form-item label="Display name" prop="name">
          <el-input v-model="form.name" placeholder="Acme Inc." />
        </el-form-item>
        <el-form-item label="Contact email" prop="contactEmail">
          <el-input v-model="form.contactEmail" placeholder="ops@acme.dev" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">Cancel</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">
          {{ dialogMode === 'create' ? 'Create tenant' : 'Save changes' }}
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

.field-hint {
  margin-top: 4px;
  font-size: 11px;
  color: #98a2b3;
  line-height: 1.4;
}

@media (max-width: 640px) {
  .search {
    width: 100%;
  }
}
</style>
