<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { auditApi } from '@/api/audit'
import type { AuditLog, AuditLogQuery } from '@/api/types'
import { formatDateTime } from '@/utils/format'

type OutcomeFilter = 'all' | 'success' | 'failure'

const ACTION_OPTIONS = [
  'AUTH_LOGIN',
  'AUTH_LOGOUT',
  'TENANT_CREATE',
  'TENANT_UPDATE',
  'TENANT_DELETE',
  'USER_CREATE',
  'USER_UPDATE',
  'USER_DELETE',
  'CUSTOMER_CREATE',
  'CUSTOMER_UPDATE',
  'CUSTOMER_DELETE',
  'ORDER_CHECKOUT',
  'RBAC_UPDATE'
]

const loading = ref(false)

const rows = ref<AuditLog[]>([])

const total = ref(0)

const page = ref(1)

const size = ref(10)

const action = ref('')

const username = ref('')

const outcome = ref<OutcomeFilter>('all')

function buildQuery(): AuditLogQuery {
  return {
    action: action.value.trim() || undefined,
    username: username.value.trim() || undefined,
    success: outcome.value === 'all' ? undefined : outcome.value === 'success'
  }
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const result = await auditApi.page(page.value, size.value, buildQuery())
    rows.value = result.records
    total.value = result.total
  } finally {
    loading.value = false
  }
}

function search(): void {
  if (page.value === 1) {
    void load()
    return
  }
  page.value = 1
}

function reset(): void {
  action.value = ''
  username.value = ''
  outcome.value = 'all'
  search()
}

/** Pretty prints the sanitised payload so reviewers can read it in place. */
function prettyArgs(value: string | null): string {
  if (!value) {
    return '—'
  }
  try {
    return JSON.stringify(JSON.parse(value), null, 2)
  } catch {
    return value
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
        <h1 class="page-title">Audit trail</h1>
        <p class="page-subtitle">
          Tenant scoped record of privileged operations. Credentials and tokens are masked before
          they are stored.
        </p>
      </div>
      <el-button text :icon="'Refresh'" @click="load">Refresh</el-button>
    </div>

    <section class="panel">
      <div class="panel-header">
        <div class="toolbar">
          <el-select
            v-model="action"
            placeholder="Action"
            clearable
            filterable
            allow-create
            class="filter-action"
          >
            <el-option v-for="option in ACTION_OPTIONS" :key="option" :label="option" :value="option" />
          </el-select>
          <el-input
            v-model="username"
            placeholder="Username"
            clearable
            class="filter-user"
            @keyup.enter="search"
            @clear="search"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <el-select v-model="outcome" class="filter-outcome">
            <el-option label="All outcomes" value="all" />
            <el-option label="Succeeded" value="success" />
            <el-option label="Failed" value="failure" />
          </el-select>
          <el-button type="primary" @click="search">Search</el-button>
          <el-button @click="reset">Reset</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="rows" empty-text="No audit entries match the filters">
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="audit-detail">
              <div class="audit-meta">
                <span>Client <span class="mono">{{ row.ipAddress || 'unknown' }}</span></span>
                <span>Duration <span class="mono">{{ row.durationMs ?? 0 }} ms</span></span>
                <span>Actor id <span class="mono">{{ row.userId ?? '—' }}</span></span>
              </div>
              <div class="audit-detail__block">
                <span class="audit-detail__label">Sanitised request arguments</span>
                <pre class="audit-json">{{ prettyArgs(row.requestArgs) }}</pre>
              </div>
              <div v-if="row.errorMessage" class="audit-detail__block">
                <span class="audit-detail__label">Error</span>
                <pre class="audit-json audit-json--error">{{ row.errorMessage }}</pre>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="Time" width="180">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="Actor" min-width="150">
          <template #default="{ row }">
            <span class="mono">{{ row.username || 'system' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="Action" min-width="170">
          <template #default="{ row }">
            <el-tag size="small" effect="plain" class="action-tag">{{ row.action }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="resource" label="Resource" width="120" />
        <el-table-column label="Request" min-width="240">
          <template #default="{ row }">
            <div class="request-cell">
              <el-tag size="small" effect="plain" type="info">{{ row.httpMethod }}</el-tag>
              <span class="mono request-uri">{{ row.uri }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="Duration" width="100" align="right">
          <template #default="{ row }">
            <span class="mono">{{ row.durationMs ?? 0 }} ms</span>
          </template>
        </el-table-column>
        <el-table-column label="Outcome" width="110">
          <template #default="{ row }">
            <el-tag :type="row.success ? 'success' : 'danger'" size="small" effect="light">
              {{ row.success ? 'Succeeded' : 'Failed' }}
            </el-tag>
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
  </div>
</template>

<style scoped>
.filter-action {
  width: 190px;
}

.filter-user {
  width: 200px;
}

.filter-outcome {
  width: 150px;
}

.action-tag {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, 'Liberation Mono', monospace;
  font-size: 11px;
}

.request-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.request-uri {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.audit-detail {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 4px 16px 12px 48px;
}

.audit-meta {
  display: flex;
  gap: 20px;
  font-size: 12px;
  color: #98a2b3;
}

.audit-detail__block {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.audit-detail__label {
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #98a2b3;
}

.audit-json {
  margin: 0;
  padding: 12px 14px;
  background: #0f172a;
  border-radius: 10px;
  color: #d7e2fb;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, 'Liberation Mono', monospace;
  font-size: 12px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
}

.audit-json--error {
  background: #fef3f2;
  color: #b42318;
}

@media (max-width: 768px) {
  .filter-action,
  .filter-user,
  .filter-outcome {
    width: 100%;
  }

  .audit-detail {
    padding-left: 16px;
  }
}
</style>
