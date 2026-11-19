<script setup lang="ts">
import { useRouter } from 'vue-router'
import { readToken } from '@/utils/session'

const router = useRouter()

const authenticated = Boolean(readToken())

function goBack(): void {
  void router.replace(authenticated ? '/dashboard' : '/login')
}
</script>

<template>
  <div class="not-found">
    <p class="code">404</p>
    <h1>Page not found</h1>
    <p class="lede">The page you requested does not exist or was moved.</p>
    <el-button type="primary" @click="goBack">
      {{ authenticated ? 'Back to dashboard' : 'Go to sign in' }}
    </el-button>
  </div>
</template>

<style scoped>
.not-found {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  gap: 8px;
  padding: 24px;
  text-align: center;
}

.code {
  margin: 0;
  font-size: 64px;
  font-weight: 700;
  letter-spacing: -0.04em;
  color: #2f6bf3;
}

h1 {
  margin: 0;
  font-size: 20px;
}

.lede {
  margin: 0 0 16px;
  font-size: 13px;
  color: var(--app-muted);
}
</style>
