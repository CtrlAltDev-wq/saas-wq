<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { subscriptionApi } from '@/api/subscription'
import type { Subscription } from '@/api/types'
import { planLabel, subscriptionStatusTag } from '@/utils/format'

const route = useRoute()

const subscription = ref<Subscription | null>(null)

const sessionId = typeof route.query.session_id === 'string' ? route.query.session_id : ''

onMounted(async () => {
  subscription.value = await subscriptionApi.current(true).catch(() => null)
})
</script>

<template>
  <div class="result">
    <div class="panel result-card">
      <div class="badge">
        <el-icon :size="26"><CircleCheckFilled /></el-icon>
      </div>
      <h1 class="title">Payment received</h1>
      <p class="lede">
        Stripe has confirmed the checkout. Your plan is activated as soon as the signed webhook is
        applied, usually within a few seconds.
      </p>

      <dl class="summary">
        <div>
          <dt>Checkout session</dt>
          <dd class="mono">{{ sessionId || '—' }}</dd>
        </div>
        <div>
          <dt>Current plan</dt>
          <dd>{{ planLabel(subscription?.planCode) }}</dd>
        </div>
        <div>
          <dt>Subscription status</dt>
          <dd>
            <el-tag
              v-if="subscription"
              :type="subscriptionStatusTag(subscription.status)"
              size="small"
              effect="light"
            >
              {{ subscription.status }}
            </el-tag>
            <span v-else class="muted">Pending webhook</span>
          </dd>
        </div>
      </dl>

      <div class="actions">
        <el-button @click="$router.push('/dashboard')">Back to dashboard</el-button>
        <el-button type="primary" @click="$router.push('/orders')">View orders</el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.result {
  display: flex;
  justify-content: center;
  padding: 40px 0;
}

.result-card {
  max-width: 520px;
  width: 100%;
  padding: 40px;
  text-align: center;
}

.badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: #e7f8f4;
  color: #0e9384;
}

.title {
  margin: 20px 0 0;
  font-size: 24px;
  letter-spacing: -0.02em;
}

.lede {
  margin: 10px auto 0;
  max-width: 400px;
  font-size: 13px;
  line-height: 1.6;
  color: var(--app-muted);
}

.summary {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin: 28px 0;
  padding: 18px 20px;
  text-align: left;
  border-radius: 12px;
  background: #f7f9fd;
}

.summary dt {
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: #98a2b3;
  margin-bottom: 4px;
}

.summary dd {
  margin: 0;
  font-size: 13px;
  color: #344054;
  word-break: break-all;
}

.actions {
  display: flex;
  justify-content: center;
  gap: 10px;
  flex-wrap: wrap;
}
</style>
