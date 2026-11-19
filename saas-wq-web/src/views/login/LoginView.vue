<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

interface LoginForm {
  tenantCode: string
  username: string
  password: string
}

interface DemoAccount {
  label: string
  tenantCode: string
  username: string
}

const DEMO_ACCOUNTS: DemoAccount[] = [
  { label: 'Platform admin', tenantCode: 'platform', username: 'superadmin' },
  { label: 'Acme administrator', tenantCode: 'acme', username: 'acme.admin' },
  { label: 'Acme member', tenantCode: 'acme', username: 'acme.member' },
  { label: 'Globex administrator', tenantCode: 'globex', username: 'globex.admin' }
]

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const formRef = ref<FormInstance>()

const loading = ref(false)

const form = reactive<LoginForm>({ tenantCode: '', username: '', password: '' })

const rules: FormRules<LoginForm> = {
  tenantCode: [
    { required: true, message: 'Tenant code is required', trigger: 'blur' },
    {
      pattern: /^[a-z0-9][a-z0-9-]{2,63}$/,
      message: 'Use 3-64 lowercase letters, digits or hyphens',
      trigger: 'blur'
    }
  ],
  username: [
    { required: true, message: 'Username is required', trigger: 'blur' },
    { min: 3, max: 64, message: 'Username must be 3-64 characters', trigger: 'blur' }
  ],
  password: [
    { required: true, message: 'Password is required', trigger: 'blur' },
    { min: 8, message: 'Password must be at least 8 characters', trigger: 'blur' }
  ]
}

function useDemo(account: DemoAccount): void {
  form.tenantCode = account.tenantCode
  form.username = account.username
  form.password = 'Admin@123'
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
  loading.value = true
  try {
    const user = await auth.login({ ...form })
    ElMessage.success(`Welcome back, ${user.nickname || user.username}`)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : ''
    await router.replace(redirect.startsWith('/') ? redirect : '/dashboard')
  } catch {
    // The response interceptor already surfaced the failure.
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login">
    <section class="login-intro">
      <div class="intro-brand">
        <div class="brand-mark">wq</div>
        <span>saas-wq</span>
      </div>
      <div class="intro-copy">
        <p class="eyebrow">Multi-tenant SaaS console</p>
        <h1>Run every tenant, subscription and payment from one workspace.</h1>
        <p class="lede">
          Row level tenant isolation, Stripe subscription billing with signed webhooks and a
          real time operations view — all behind a single sign on.
        </p>
        <ul class="intro-points">
          <li><span class="check">01</span>Tenant scoped data access enforced in SQL</li>
          <li><span class="check">02</span>Checkout, invoices and dunning in one place</li>
          <li><span class="check">03</span>Role aware navigation for admins and members</li>
        </ul>
      </div>
      <p class="intro-foot">Spring Boot 3 · Vue 3 · Stripe</p>
    </section>

    <section class="login-panel">
      <div class="form-card">
        <header class="form-head">
          <h2>Sign in</h2>
          <p>Use your tenant code and workspace credentials.</p>
        </header>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="submit">
          <el-form-item label="Tenant code" prop="tenantCode">
            <el-input v-model="form.tenantCode" placeholder="acme" autocomplete="organization" />
          </el-form-item>
          <el-form-item label="Username" prop="username">
            <el-input v-model="form.username" placeholder="acme.admin" autocomplete="username" />
          </el-form-item>
          <el-form-item label="Password" prop="password">
            <el-input
              v-model="form.password"
              type="password"
              show-password
              placeholder="••••••••"
              autocomplete="current-password"
              @keyup.enter="submit"
            />
          </el-form-item>
          <el-button type="primary" class="submit" :loading="loading" @click="submit">
            Sign in
          </el-button>
        </el-form>

        <div class="demo">
          <div class="demo-head">
            <span>Demo accounts</span>
            <span class="muted">password Admin@123</span>
          </div>
          <div class="demo-grid">
            <button
              v-for="account in DEMO_ACCOUNTS"
              :key="account.username"
              type="button"
              class="demo-item"
              @click="useDemo(account)"
            >
              <span class="demo-label">{{ account.label }}</span>
              <span class="demo-user mono">{{ account.tenantCode }} / {{ account.username }}</span>
            </button>
          </div>
        </div>
      </div>
      <p class="copyright">© {{ new Date().getFullYear() }} saas-wq. Internal demonstration build.</p>
    </section>
  </div>
</template>

<style scoped>
.login {
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(0, 1fr);
  min-height: 100vh;
  background: #ffffff;
}

.login-intro {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 44px 56px;
  color: #ffffff;
  background: radial-gradient(circle at 15% 20%, rgba(79, 139, 255, 0.35), transparent 42%),
    radial-gradient(circle at 85% 85%, rgba(47, 107, 243, 0.4), transparent 45%), #0b1220;
  overflow: hidden;
}

.login-intro::after {
  content: '';
  position: absolute;
  inset: 0;
  background-image: linear-gradient(rgba(255, 255, 255, 0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.05) 1px, transparent 1px);
  background-size: 44px 44px;
  mask-image: radial-gradient(circle at 30% 30%, #000 0%, transparent 72%);
  pointer-events: none;
}

.intro-brand {
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 16px;
  font-weight: 650;
}

.brand-mark {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: linear-gradient(135deg, #2f6bf3, #4f8bff);
  font-size: 14px;
  font-weight: 700;
}

.intro-copy {
  position: relative;
  max-width: 520px;
}

.eyebrow {
  margin: 0 0 18px;
  font-size: 11px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: #8fb4ff;
}

.intro-copy h1 {
  margin: 0;
  font-size: 38px;
  line-height: 1.18;
  letter-spacing: -0.02em;
  font-weight: 650;
}

.lede {
  margin: 20px 0 0;
  font-size: 14px;
  line-height: 1.7;
  color: #b6c4da;
}

.intro-points {
  margin: 34px 0 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.intro-points li {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 13px;
  color: #d6e0ef;
}

.check {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: rgba(79, 139, 255, 0.16);
  color: #9cc0ff;
  font-size: 11px;
  font-weight: 600;
  flex-shrink: 0;
}

.intro-foot {
  position: relative;
  margin: 0;
  font-size: 11px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: #5f7089;
}

.login-panel {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 26px;
  padding: 40px 24px;
}

.form-card {
  width: 100%;
  max-width: 400px;
}

.form-head h2 {
  margin: 0;
  font-size: 26px;
  letter-spacing: -0.02em;
}

.form-head p {
  margin: 8px 0 26px;
  color: var(--app-muted);
  font-size: 13px;
}

.submit {
  width: 100%;
  height: 42px;
  margin-top: 4px;
  font-weight: 600;
}

.demo {
  margin-top: 30px;
  padding-top: 22px;
  border-top: 1px solid var(--app-line);
}

.demo-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  font-size: 12px;
  font-weight: 600;
  color: #475467;
}

.demo-head .muted {
  font-weight: 400;
}

.demo-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.demo-item {
  display: flex;
  flex-direction: column;
  gap: 3px;
  padding: 10px 12px;
  text-align: left;
  border: 1px solid var(--app-line);
  border-radius: 10px;
  background: #ffffff;
  cursor: pointer;
  transition: border-color 0.15s ease, background 0.15s ease;
}

.demo-item:hover {
  border-color: #b9cdf8;
  background: #f6f9ff;
}

.demo-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--app-ink);
}

.demo-user {
  font-size: 11px;
}

.copyright {
  margin: 0;
  font-size: 11px;
  color: #98a2b3;
}

@media (max-width: 960px) {
  .login {
    grid-template-columns: 1fr;
  }

  .login-intro {
    display: none;
  }

  .login-panel {
    min-height: 100vh;
    background: #f7f9fc;
  }

  .form-card {
    padding: 28px;
    background: #ffffff;
    border-radius: 16px;
    border: 1px solid var(--app-line);
    box-shadow: 0 8px 30px rgba(16, 24, 40, 0.06);
  }
}
</style>
