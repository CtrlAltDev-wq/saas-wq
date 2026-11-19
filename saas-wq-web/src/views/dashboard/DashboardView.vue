<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { customerApi } from '@/api/customer'
import { orderApi } from '@/api/order'
import { subscriptionApi } from '@/api/subscription'
import { tenantApi } from '@/api/tenant'
import type { Order, Subscription } from '@/api/types'
import { useEcharts } from '@/composables/useEcharts'
import { useAuthStore } from '@/stores/auth'
import { useTrafficStore } from '@/stores/traffic'
import { formatDate, formatDateTime, formatMoney, orderStatusTag, planLabel } from '@/utils/format'

interface StatCard {
  label: string
  value: string
  hint: string
  icon: string
  tone: string
}

const auth = useAuthStore()
const traffic = useTrafficStore()

const loading = ref(false)

const orders = ref<Order[]>([])

const revenueCents = ref(0)

const paidCount = ref(0)

const pendingCount = ref(0)

const customerTotal = ref(0)

const activeTenants = ref(0)

const tenantTotal = ref(0)

const subscription = ref<Subscription | null>(null)

const chartEl = ref<HTMLElement | null>(null)

const { setOption } = useEcharts(chartEl)

const currency = computed(() => orders.value[0]?.currency ?? 'usd')

const recentOrders = computed(() => orders.value.slice(0, 5))

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 12) {
    return 'Good morning'
  }
  if (hour < 18) {
    return 'Good afternoon'
  }
  return 'Good evening'
})

const today = new Intl.DateTimeFormat('en-GB', { dateStyle: 'full' }).format(new Date())

const stats = computed<StatCard[]>(() => {
  const cards: StatCard[] = [
    {
      label: 'Total revenue',
      value: formatMoney(revenueCents.value, currency.value),
      hint: `${paidCount.value} paid order(s) in this tenant`,
      icon: 'Wallet',
      tone: 'blue'
    }
  ]

  if (auth.isSuperAdmin) {
    cards.push({
      label: 'Active tenants',
      value: String(activeTenants.value),
      hint: `${tenantTotal.value} registered tenant(s)`,
      icon: 'OfficeBuilding',
      tone: 'violet'
    })
  } else {
    cards.push({
      label: 'Current plan',
      value: planLabel(subscription.value?.planCode),
      hint: subscription.value
        ? `Period ends ${formatDate(subscription.value.currentPeriodEnd)}`
        : 'No active subscription',
      icon: 'Medal',
      tone: 'violet'
    })
  }

  cards.push(
    {
      label: 'Customers',
      value: String(customerTotal.value),
      hint: 'Tenant scoped directory',
      icon: 'UserFilled',
      tone: 'teal'
    },
    {
      label: 'Awaiting payment',
      value: String(pendingCount.value),
      hint: `${orders.value.length} order(s) recorded`,
      icon: 'Clock',
      tone: 'amber'
    }
  )

  return cards
})

/** Loads up to 500 recent orders so revenue covers the visible billing history. */
async function loadOrders(): Promise<Order[]> {
  const collected: Order[] = []
  for (let page = 1; page <= 5; page += 1) {
    const result = await orderApi.page(page, 100)
    collected.push(...result.records)
    if (page >= result.pages || result.records.length === 0) {
      break
    }
  }
  return collected
}

function renderChart(): void {
  const { labels, values } = traffic.series
  setOption({
    grid: { top: 20, right: 20, bottom: 30, left: 46 },
    tooltip: {
      trigger: 'axis',
      backgroundColor: '#101828',
      borderWidth: 0,
      textStyle: { color: '#ffffff', fontSize: 12 },
      axisPointer: { type: 'line', lineStyle: { color: '#c6d8fc' } }
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: labels,
      axisLine: { lineStyle: { color: '#e8ecf4' } },
      axisTick: { show: false },
      axisLabel: { color: '#98a2b3', fontSize: 11, interval: 4 }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      splitLine: { lineStyle: { color: '#eef1f7' } },
      axisLabel: { color: '#98a2b3', fontSize: 11 }
    },
    series: [
      {
        name: 'API calls',
        type: 'line',
        smooth: true,
        showSymbol: false,
        data: values,
        lineStyle: { width: 2.4, color: '#2f6bf3' },
        itemStyle: { color: '#2f6bf3' },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(47, 107, 243, 0.22)' },
              { offset: 1, color: 'rgba(47, 107, 243, 0.02)' }
            ]
          }
        }
      }
    ]
  })
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const loaded = await loadOrders()
    orders.value = loaded
    const paid = loaded.filter((order) => order.status === 'PAID')
    revenueCents.value = paid.reduce((sum, order) => sum + order.amountCents, 0)
    paidCount.value = paid.length
    pendingCount.value = loaded.filter((order) => order.status === 'PENDING').length

    const customers = await customerApi.page(1, 1)
    customerTotal.value = customers.total

    if (auth.isSuperAdmin) {
      const tenants = await tenantApi.page(1, 100)
      tenantTotal.value = tenants.total
      activeTenants.value = tenants.records.filter((tenant) => tenant.status === 'ACTIVE').length
    } else {
      subscription.value = await subscriptionApi.current(true).catch(() => null)
    }
  } finally {
    loading.value = false
  }
  renderChart()
}

let clock: number | undefined

onMounted(async () => {
  renderChart()
  await load()
  clock = window.setInterval(() => {
    traffic.pulse()
    renderChart()
  }, 60_000)
})

onBeforeUnmount(() => {
  if (clock) {
    window.clearInterval(clock)
  }
})

watch(
  () => traffic.series,
  () => renderChart()
)
</script>

<template>
  <div v-loading="loading" class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">{{ greeting }}, {{ auth.displayName }}</h1>
        <p class="page-subtitle">
          {{ auth.isSuperAdmin ? 'Platform overview' : 'Workspace overview' }} · {{ today }}
        </p>
      </div>
      <el-button :icon="'Refresh'" @click="load">Refresh</el-button>
    </div>

    <div class="stats">
      <article v-for="card in stats" :key="card.label" class="stat">
        <div class="stat-icon" :class="`stat-icon--${card.tone}`">
          <el-icon :size="18"><component :is="card.icon" /></el-icon>
        </div>
        <div class="stat-copy">
          <span class="stat-label">{{ card.label }}</span>
          <span class="stat-value">{{ card.value }}</span>
          <span class="stat-hint">{{ card.hint }}</span>
        </div>
      </article>
    </div>

    <section class="panel">
      <div class="panel-header">
        <div>
          <h2 class="panel-title">API calls</h2>
          <p class="panel-caption">Rolling 30 minute window from this browser session</p>
        </div>
        <span class="pill">{{ traffic.total }} calls</span>
      </div>
      <div ref="chartEl" class="chart-body" />
    </section>

    <section class="panel">
      <div class="panel-header">
        <div>
          <h2 class="panel-title">Recent orders</h2>
          <p class="panel-caption">Latest payment activity of the current tenant</p>
        </div>
        <router-link to="/orders" class="panel-link">View all</router-link>
      </div>
      <el-table :data="recentOrders" empty-text="No orders recorded yet">
        <el-table-column prop="orderNo" label="Order" min-width="180">
          <template #default="{ row }">
            <span class="mono">{{ row.orderNo }}</span>
          </template>
        </el-table-column>
        <el-table-column label="Plan" width="140">
          <template #default="{ row }">{{ planLabel(row.planCode) }}</template>
        </el-table-column>
        <el-table-column label="Amount" width="130">
          <template #default="{ row }">{{ formatMoney(row.amountCents, row.currency) }}</template>
        </el-table-column>
        <el-table-column label="Status" width="120">
          <template #default="{ row }">
            <el-tag :type="orderStatusTag(row.status)" size="small" effect="light">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Created" min-width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
      </el-table>
    </section>
  </div>
</template>

<style scoped>
.stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.stat {
  display: flex;
  gap: 14px;
  padding: 18px;
  background: #ffffff;
  border: 1px solid var(--app-line);
  border-radius: 14px;
  box-shadow: 0 1px 2px rgba(16, 24, 40, 0.04);
}

.stat-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border-radius: 10px;
  flex-shrink: 0;
}

.stat-icon--blue {
  background: #eef3fe;
  color: #2f6bf3;
}

.stat-icon--violet {
  background: #f2effe;
  color: #7a5af8;
}

.stat-icon--teal {
  background: #e7f8f4;
  color: #0e9384;
}

.stat-icon--amber {
  background: #fef4e6;
  color: #dc6803;
}

.stat-copy {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.stat-label {
  font-size: 12px;
  color: var(--app-muted);
  font-weight: 500;
}

.stat-value {
  font-size: 24px;
  font-weight: 650;
  letter-spacing: -0.02em;
  line-height: 1.2;
}

.stat-hint {
  font-size: 11px;
  color: #98a2b3;
}

.panel-header > div {
  min-width: 0;
}

.panel-caption {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--app-muted);
}

.panel-link {
  font-size: 12px;
  font-weight: 600;
  color: #2f6bf3;
}

.pill {
  padding: 4px 10px;
  border-radius: 999px;
  background: #f2f5fa;
  font-size: 11px;
  font-weight: 600;
  color: #475467;
}

.chart-body {
  height: 320px;
  padding: 8px 8px 4px;
}

@media (max-width: 1200px) {
  .stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .stats {
    grid-template-columns: 1fr;
  }

  .chart-body {
    height: 260px;
  }
}
</style>
