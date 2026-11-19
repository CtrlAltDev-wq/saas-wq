<script setup lang="ts">
import { nextTick, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { customerApi } from '@/api/customer'
import type { Customer, CustomerStatus } from '@/api/types'
import { accountStatusTag, formatDate } from '@/utils/format'

interface CustomerForm {
  name: string
  company: string
  email: string
  phone: string
  status: CustomerStatus
  remark: string
}

const loading = ref(false)

const rows = ref<Customer[]>([])

const total = ref(0)

const page = ref(1)

const size = ref(10)

const keyword = ref('')

const dialogVisible = ref(false)

const dialogMode = ref<'create' | 'edit'>('create')

const editingId = ref<number | null>(null)

const submitting = ref(false)

const formRef = ref<FormInstance>()

const form = reactive<CustomerForm>({
  name: '',
  company: '',
  email: '',
  phone: '',
  status: 'ACTIVE',
  remark: ''
})

const rules: FormRules<CustomerForm> = {
  name: [
    { required: true, message: 'Customer name is required', trigger: 'blur' },
    { max: 128, message: 'Customer name must not exceed 128 characters', trigger: 'blur' }
  ],
  company: [{ max: 128, message: 'Company must not exceed 128 characters', trigger: 'blur' }],
  email: [{ type: 'email', message: 'Email is invalid', trigger: 'blur' }],
  phone: [{ max: 32, message: 'Phone must not exceed 32 characters', trigger: 'blur' }],
  remark: [{ max: 512, message: 'Remark must not exceed 512 characters', trigger: 'blur' }]
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const result = await customerApi.page(page.value, size.value, keyword.value.trim())
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
  form.name = ''
  form.company = ''
  form.email = ''
  form.phone = ''
  form.status = 'ACTIVE'
  form.remark = ''
  dialogVisible.value = true
  void nextTick(() => formRef.value?.clearValidate())
}

function openEdit(row: Customer): void {
  dialogMode.value = 'edit'
  editingId.value = row.id
  form.name = row.name
  form.company = row.company ?? ''
  form.email = row.email ?? ''
  form.phone = row.phone ?? ''
  form.status = row.status
  form.remark = row.remark ?? ''
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
    const payload = {
      name: form.name.trim(),
      company: form.company.trim() || undefined,
      email: form.email.trim() || undefined,
      phone: form.phone.trim() || undefined,
      status: form.status,
      remark: form.remark.trim() || undefined
    }
    if (dialogMode.value === 'create') {
      await customerApi.create(payload)
      ElMessage.success('Customer created')
    } else if (editingId.value !== null) {
      await customerApi.update(editingId.value, payload)
      ElMessage.success('Customer updated')
    }
    dialogVisible.value = false
    await load()
  } catch {
    // The response interceptor already surfaced the failure.
  } finally {
    submitting.value = false
  }
}

async function remove(row: Customer): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `Delete customer "${row.name}"? Historical orders are kept for reconciliation.`,
      'Delete customer',
      { type: 'warning', confirmButtonText: 'Delete', cancelButtonText: 'Cancel' }
    )
  } catch {
    return
  }
  try {
    await customerApi.remove(row.id)
    ElMessage.success('Customer deleted')
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
        <h1 class="page-title">Customers</h1>
        <p class="page-subtitle">
          Billing contacts of the current tenant. Stripe ids are attached on the first checkout.
        </p>
      </div>
      <el-button type="primary" @click="openCreate">
        <el-icon><Plus /></el-icon>
        <span>New customer</span>
      </el-button>
    </div>

    <section class="panel">
      <div class="panel-header">
        <div class="toolbar">
          <el-input
            v-model="keyword"
            placeholder="Search name or company"
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

      <el-table v-loading="loading" :data="rows" empty-text="No customers yet">
        <el-table-column prop="name" label="Name" min-width="150" />
        <el-table-column prop="company" label="Company" min-width="160">
          <template #default="{ row }">{{ row.company || '—' }}</template>
        </el-table-column>
        <el-table-column prop="email" label="Email" min-width="200">
          <template #default="{ row }">{{ row.email || '—' }}</template>
        </el-table-column>
        <el-table-column prop="phone" label="Phone" width="150">
          <template #default="{ row }">{{ row.phone || '—' }}</template>
        </el-table-column>
        <el-table-column label="Stripe customer" min-width="190">
          <template #default="{ row }">
            <span v-if="row.stripeCustomerId" class="mono">{{ row.stripeCustomerId }}</span>
            <span v-else class="muted">Not linked</span>
          </template>
        </el-table-column>
        <el-table-column label="Status" width="110">
          <template #default="{ row }">
            <el-tag :type="accountStatusTag(row.status)" size="small" effect="light">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="Remark" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ row.remark || '—' }}</template>
        </el-table-column>
        <el-table-column label="Created" width="140">
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
      :title="dialogMode === 'create' ? 'New customer' : 'Edit customer'"
      width="520px"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="form-grid">
          <el-form-item label="Name" prop="name">
            <el-input v-model="form.name" placeholder="Jane Cooper" />
          </el-form-item>
          <el-form-item label="Company" prop="company">
            <el-input v-model="form.company" placeholder="Acme Inc." />
          </el-form-item>
          <el-form-item label="Email" prop="email">
            <el-input v-model="form.email" placeholder="jane@acme.dev" />
          </el-form-item>
          <el-form-item label="Phone" prop="phone">
            <el-input v-model="form.phone" placeholder="+1 555 0100" />
          </el-form-item>
        </div>
        <el-form-item label="Status" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio-button value="ACTIVE">Active</el-radio-button>
            <el-radio-button value="INACTIVE">Inactive</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="Remark" prop="remark">
          <el-input
            v-model="form.remark"
            type="textarea"
            :rows="3"
            maxlength="512"
            show-word-limit
            placeholder="Renewal notes, billing preferences…"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">Cancel</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">
          {{ dialogMode === 'create' ? 'Create customer' : 'Save changes' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.search {
  width: 280px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
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
