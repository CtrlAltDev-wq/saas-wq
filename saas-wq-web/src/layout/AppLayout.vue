<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import SidebarNav from '@/layout/SidebarNav.vue'
import { useAppStore } from '@/stores/app'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const app = useAppStore()
const auth = useAuthStore()

const isMobile = ref(false)

const asideWidth = computed(() => (app.sidebarCollapsed ? '64px' : '232px'))

const roleLabel = computed(() => {
  if (auth.role === 'SUPER_ADMIN') {
    return 'Platform admin'
  }
  if (auth.role === 'TENANT_ADMIN') {
    return 'Tenant admin'
  }
  return 'Member'
})

const breadcrumbs = computed(() => {
  const items: string[] = ['Home']
  if (route.meta.group && route.meta.group !== 'Overview') {
    items.push(route.meta.group)
  }
  if (route.meta.title) {
    items.push(route.meta.title)
  }
  return items
})

function handleResize(): void {
  const width = window.innerWidth
  isMobile.value = width < 768
  if (!isMobile.value && width < 1200) {
    app.setSidebarCollapsed(true)
  }
  if (!isMobile.value) {
    app.setMobileMenuOpen(false)
  }
}

function toggleNav(): void {
  if (isMobile.value) {
    app.setMobileMenuOpen(!app.mobileMenuOpen)
    return
  }
  app.toggleSidebar()
}

async function signOut(): Promise<void> {
  try {
    await ElMessageBox.confirm('You will be returned to the sign in screen.', 'Sign out', {
      confirmButtonText: 'Sign out',
      cancelButtonText: 'Cancel',
      type: 'warning'
    })
  } catch {
    return
  }
  await auth.logout()
  await router.replace({ name: 'login' })
}

function handleCommand(command: string | number | object): void {
  if (command === 'signout') {
    void signOut()
  }
}

onMounted(() => {
  handleResize()
  window.addEventListener('resize', handleResize)
  if (!auth.user) {
    void auth.fetchProfile().catch(() => undefined)
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
})

watch(
  () => route.fullPath,
  () => {
    app.setMobileMenuOpen(false)
  }
)
</script>

<template>
  <el-container class="shell">
    <el-aside v-if="!isMobile" :width="asideWidth" class="aside">
      <SidebarNav :collapsed="app.sidebarCollapsed" />
    </el-aside>

    <el-drawer
      v-if="isMobile"
      v-model="app.mobileMenuOpen"
      direction="ltr"
      size="248px"
      :with-header="false"
      class="mobile-nav"
    >
      <SidebarNav />
    </el-drawer>

    <el-container class="body">
      <el-header height="64px" class="topbar">
        <div class="topbar-left">
          <el-button text class="icon-button" aria-label="Toggle navigation" @click="toggleNav">
            <el-icon :size="20">
              <Fold v-if="!isMobile && !app.sidebarCollapsed" />
              <Expand v-else />
            </el-icon>
          </el-button>
          <el-breadcrumb separator="/" class="crumbs">
            <el-breadcrumb-item
              v-for="(crumb, index) in breadcrumbs"
              :key="`${crumb}-${index}`"
              :to="index === 0 ? '/dashboard' : undefined"
            >
              {{ crumb }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="topbar-right">
          <el-tag size="small" effect="plain" round class="role-tag">{{ roleLabel }}</el-tag>
          <el-dropdown trigger="click" @command="handleCommand">
            <div class="user">
              <el-avatar :size="32" class="avatar">{{ auth.initials }}</el-avatar>
              <div class="user-copy">
                <span class="user-name">{{ auth.displayName }}</span>
                <span class="user-meta">{{ auth.user?.username }}</span>
              </div>
              <el-icon class="chevron"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled>{{ auth.user?.email || 'No email on file' }}</el-dropdown-item>
                <el-dropdown-item divided command="signout">Sign out</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.shell {
  height: 100vh;
  overflow: hidden;
}

.aside {
  transition: width 0.2s ease;
  overflow: hidden;
}

.body {
  min-width: 0;
  background: var(--app-surface);
}

.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 0 24px;
  background: #ffffff;
  border-bottom: 1px solid var(--app-line);
}

.topbar-left {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.icon-button {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  color: #475467;
}

.icon-button:hover {
  background: #f2f5fa;
}

.crumbs {
  font-size: 13px;
}

.crumbs :deep(.el-breadcrumb__inner) {
  color: var(--app-muted);
  font-weight: 500;
}

.crumbs :deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) {
  color: var(--app-ink);
  font-weight: 600;
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 14px;
}

.role-tag {
  border-color: #d7e2fb;
  color: #2f6bf3;
  background: #f4f8ff;
}

.user {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 4px 8px 4px 4px;
  border-radius: 10px;
  cursor: pointer;
  outline: none;
}

.user:hover {
  background: #f4f6fb;
}

.avatar {
  background: linear-gradient(135deg, #2f6bf3, #6f9bf7);
  font-size: 13px;
  font-weight: 600;
}

.user-copy {
  display: flex;
  flex-direction: column;
  line-height: 1.25;
}

.user-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--app-ink);
}

.user-meta {
  font-size: 11px;
  color: var(--app-muted);
}

.chevron {
  color: #98a2b3;
  font-size: 12px;
}

.main {
  padding: 24px;
  overflow-y: auto;
}

.mobile-nav :deep(.el-drawer__body) {
  padding: 0;
}

@media (max-width: 768px) {
  .topbar {
    padding: 0 14px;
  }

  .main {
    padding: 16px;
  }

  .user-copy,
  .role-tag {
    display: none;
  }
}
</style>
