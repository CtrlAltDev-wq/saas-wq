<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { customerApi } from '@/api/customer'
import { orderApi } from '@/api/order'
import { subscriptionApi } from '@/api/subscription'
import type { Customer, Order, Subscription } from '@/api/types'
import { useAuthStore } from '@/stores/auth'
import {
  formatDate,
  formatDateTime,
  formatMoney,
  orderStatusTag,
  planLabel,
  subscriptionStatusTag
} from '@/utils/format'

/** Mirrors wq.stripe.plans so the dialog can quote prices before calling Stripe. */
const PLANS = [
  { code: 'STARTER', name: 'Starter', amountCents: 2900 },
  { code: 'PROFESSIONAL', name: 'Professional', amountCents: 9900 },
  { code: 'ENTERPRISE', name: 'Enterprise', amountCents: 29900 }
]

const auth = useAuthStore()

const loading = ref(false)

const subscription = ref<Subscription | null>(null)

const orders = ref<Order[]>([])

const total = ref(0)

const page = ref(1)

const size = ref(10)

const detailVisible = ref(false)

const detail = ref<Order | null>(null)

const checkoutVisible = ref(false)

const checkoutSubmitting = ref(false)

const customerOptions = ref<Customer[]>([])

const customerLoading = ref(false)

const checkoutForm = reactive<{ customerId: number | null; planCode: string }>({
  customerId: null,
  planCode: 'PROFESSIONAL'
})

async function loadOrders(): Promise<void> {
  loading.value = true
  try {
    const result = await orderApi.page(page.value, size.value)
    orders.value = result.records
    total.value = result.total
  } finally {
    loading.value = false
  }
}

async function loadSubscription(): Promise<void> {
  subscription.value = await subscriptionApi.current(true).catch(() => null)
}

async function refresh(): Promise<void> {
  await Promise.all([loadOrders(), loadSubscription()])
}

function openDetail(row: Order): void {
  detail.value = row
  detailVisible.value = true
}

async function openCheckout(): Promise<void> {
  checkoutForm.customerId = null
  checkoutForm.planCode = 'PROFESSIONAL'
  checkoutVisible.value = true
  if (customerOptions.value.length > 0) {
    return
  }
  customerLoading.value = true
  try {
    const result = await customerApi.page(1, 100)
    customerOptions.value = result.records.filter((customer) => customer.status === 'ACTIVE')
  } finally {
    customerLoading.value = false
  }
}

async function startCheckout(): Promise<void> {
  if (checkoutForm.customerId === null) {
    ElMessage.warning('Select the billing customer first.')
    return
  }
  checkoutSubmitting.value = true
  try {
    const session = await orderApi.checkout({
      customerId: checkoutForm.customerId,
      planCode: checkoutForm.planCode
    })
    checkoutVisible.value = false
    ElMessage.success(`Checkout session ${session.orderNo} created, redirecting to Stripe…`)
    window.location.assign(session.checkoutUrl)
  } catch {
    // The response interceptor already surfaced the failure.
  } finally {
    checkoutSubmitting.value = false
  }
}

const selectedPlan = () => PLANS.find((plan) => plan.code === checkoutForm.planCode)

watch([page, size], () => {
  void loadOrders()
})

onMounted(refresh)
</script>

<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">Orders &amp; Billing</h1>
        <p class="page-subtitle">
          Payment history of the current tenant and the entitlement granted by Stripe webhooks.
        </p>
      </div>
      <div class="toolbar">
        <el-button :icon="'Refresh'" @click="refresh">Refresh</el-button>
        <el-button v-if="auth.canManage" type="primary" @click="openCheckout">
          <el-icon><Plus /></el-icon>
          <span>New subscription</span>
        </el-button>
      </div>
    </div>

    <section class="panel">
      <div class="panel-header">
        <div>
          <h2 class="panel-title">Current subscription</h2>
          <p class="panel-caption">Updated by checkout and invoice webhooks</p>
        </div>
        <el-tag
          v-if="subscription"
          :type="subscriptionStatusTag(subscription.status)"
          effect="light"
          size="small"
        >
          {{ subscription.status }}
        </el-tag>
      </div>

      <div v-if="subscription" class="sub-body">
        <div class="sub-summary">
          <span class="sub-plan">{{ planLabel(subscription.planCode) }}</span>
          <span class="sub-period">
            {{ formatDate(subscription.currentPeriodStart) }} →
            {{ formatDate(subscription.currentPeriodEnd) }}
          </span>
        </div>
        <dl class="sub-meta">
          <div>
            <dt>Stripe subscription</dt>
            <dd class="mono">{{ subscription.stripeSubscriptionId || '—' }}</dd>
          </div>
          <div>
            <dt>Subscribed since</dt>
            <dd>{{ formatDate(subscription.createdAt) }}</dd>
          </div>
        </dl>
      </div>

      <div v-else class="sub-empty">
        <el-icon :size="22"><CreditCard /></el-icon>
        <div>
          <strong>No active subscription</strong>
          <p>Start a checkout to activate a plan for this tenant.</p>
        </div>
      </div>
    </section>

    <section class="panel">
      <div class="panel-header">
        <div>
          <h2 class="panel-title">Payment history</h2>
          <p class="panel-caption">{{ total }} order(s) recorded for this tenant</p>
        </div>
      </div>

      <el-table v-loading="loading" :data="orders" empty-text="No orders yet">
        <el-table-column label="Order" min-width="190">
          <template #default="{ row }">
            <span class="mono">{{ row.orderNo }}</span>
          </template>
        </el-table-column>
        <el-table-column label="Plan" width="130">
          <template #default="{ row }">{{ planLabel(row.planCode) }}</template>
        </el-table-column>
        <el-table-column label="Amount" width="120">
          <template #default="{ row }">{{ formatMoney(row.amountCents, row.currency) }}</template>
        </el-table-column>
        <el-table-column label="Status" width="110">
          <template #default="{ row }">
            <el-tag :type="orderStatusTag(row.status)" size="small" effect="light">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Customer" width="110">
          <template #default="{ row }">#{{ row.customerId }}</template>
        </el-table-column>
        <el-table-column label="Created" min-width="160">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="Paid at" min-width="160">
          <template #default="{ row }">{{ formatDateTime(row.paidAt) }}</template>
        </el-table-column>
        <el-table-column label="Actions" width="100" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="openDetail(row)">View</el-button>
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

    <el-drawer v-model="detailVisible" title="Order detail" size="420px">
      <template v-if="detail">
        <dl class="detail-list">
          <div>
            <dt>Order number</dt>
            <dd class="mono">{{ detail.orderNo }}</dd>
          </div>
          <div>
            <dt>Plan</dt>
            <dd>{{ planLabel(detail.planCode) }}</dd>
          </div>
          <div>
            <dt>Amount</dt>
            <dd>{{ formatMoney(detail.amountCents, detail.currency) }}</dd>
          </div>
          <div>
            <dt>Status</dt>
            <dd>
              <el-tag :type="orderStatusTag(detail.status)" size="small" effect="light">
                {{ detail.status }}
              </el-tag>
            </dd>
          </div>
          <div>
            <dt>Customer id</dt>
            <dd>#{{ detail.customerId }}</dd>
          </div>
          <div>
            <dt>Checkout session</dt>
            <dd class="mono">{{ detail.stripeCheckoutSessionId || '—' }}</dd>
          </div>
          <div>
            <dt>Stripe subscription</dt>
            <dd class="mono">{{ detail.stripeSubscriptionId || '—' }}</dd>
          </div>
          <div>
            <dt>Created at</dt>
            <dd>{{ formatDateTime(detail.createdAt) }}</dd>
          </div>
          <div>
            <dt>Paid at</dt>
            <dd>{{ formatDateTime(detail.paidAt) }}</dd>
          </div>
        </dl>
      </template>
    </el-drawer>

    <el-dialog v-model="checkoutVisible" title="Start a subscription" width="500px">
      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="You will be redirected to Stripe Checkout."
        description="The tenant plan is activated by the signed webhook once the payment succeeds."
        class="checkout-alert"
      />

      <el-form label-position="top">
        <el-form-item label="Billing customer">
          <el-select
            v-model="checkoutForm.customerId"
            :loading="customerLoading"
            filterable
            placeholder="Select an active customer"
            class="full"
          >
            <el-option
              v-for="customer in customerOptions"
              :key="customer.id"
              :label="customer.company ? `${customer.name} · ${customer.company}` : customer.name"
              :value="customer.id"
            />
          </el-select>
          <span v-if="!customerLoading && customerOptions.length === 0" class="field-hint">
            No active customer found. Create one in the Customers page first.
          </span>
        </el-form-item>

        <el-form-item label="Plan">
          <el-radio-group v-model="checkoutForm.planCode" class="plan-group">
            <el-radio-button v-for="plan in PLANS" :key="plan.code" :value="plan.code">
              {{ plan.name }} · {{ formatMoney(plan.amountCents) }}/mo
            </el-radio-button>
          </el-radio-group>
        </el-form-item>

        <div class="checkout-total">
          <span>Due today</span>
          <strong>{{ formatMoney(selectedPlan()?.amountCents ?? 0) }}</strong>
        </div>
      </el-form>

      <template #footer>
        <el-button @click="checkoutVisible = false">Cancel</el-button>
        <el-button
          type="primary"
          :loading="checkoutSubmitting"
          :disabled="customerOptions.length === 0"
          @click="startCheckout"
        >
          Continue to Stripe
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.sub-body {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 20px;
  flex-wrap: wrap;
}

.sub-summary {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.sub-plan {
  font-size: 22px;
  font-weight: 650;
  letter-spacing: -0.01em;
}

.sub-period {
  font-size: 12px;
  color: var(--app-muted);
}

.sub-meta {
  display: flex;
  gap: 36px;
  margin: 0;
}

.sub-meta dt,
.detail-list dt {
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: #98a2b3;
  margin-bottom: 4px;
}

.sub-meta dd {
  margin: 0;
  font-size: 13px;
  color: #344054;
}

.sub-empty {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 24px 20px;
  color: #475467;
}

.sub-empty strong {
  display: block;
  font-size: 13px;
}

.sub-empty p {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--app-muted);
}

.detail-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin: 0;
}

.detail-list dd {
  margin: 0;
  font-size: 13px;
  color: #344054;
  word-break: break-all;
}

.checkout-alert {
  margin-bottom: 18px;
}

.full {
  width: 100%;
}

.plan-group {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.plan-group :deep(.el-radio-button__inner) {
  border-radius: 8px;
  border-left: 1px solid var(--el-border-color);
}

.checkout-total {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-radius: 10px;
  background: #f7f9fd;
  font-size: 13px;
  color: #475467;
}

.checkout-total strong {
  font-size: 18px;
  color: var(--app-ink);
}

.field-hint {
  margin-top: 6px;
  font-size: 11px;
  color: #98a2b3;
}

@media (max-width: 640px) {
  .sub-meta {
    gap: 20px;
    flex-direction: column;
  }
}
</style>
