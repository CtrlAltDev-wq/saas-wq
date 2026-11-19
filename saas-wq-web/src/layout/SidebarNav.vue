<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import type { Role } from '@/api/types'

withDefaults(defineProps<{ collapsed?: boolean }>(), { collapsed: false })

interface NavItem {
  path: string
  title: string
  icon: string
  group: string
  roles?: Role[]
}

const NAV_ITEMS: NavItem[] = [
  { path: '/dashboard', title: 'Dashboard', icon: 'Odometer', group: 'Overview' },
  {
    path: '/tenants',
    title: 'Tenants',
    icon: 'OfficeBuilding',
    group: 'Platform',
    roles: ['SUPER_ADMIN']
  },
  { path: '/customers', title: 'Customers', icon: 'UserFilled', group: 'Business' },
  { path: '/orders', title: 'Orders & Billing', icon: 'CreditCard', group: 'Business' },
  {
    path: '/users',
    title: 'Users',
    icon: 'User',
    group: 'Administration',
    roles: ['SUPER_ADMIN', 'TENANT_ADMIN']
  }
]

const route = useRoute()
const auth = useAuthStore()

const visibleItems = computed(() =>
  NAV_ITEMS.filter(
    (item) => !item.roles || (auth.role !== null && item.roles.includes(auth.role))
  )
)

const sections = computed(() => {
  const grouped = new Map<string, NavItem[]>()
  visibleItems.value.forEach((item) => {
    const list = grouped.get(item.group) ?? []
    list.push(item)
    grouped.set(item.group, list)
  })
  return Array.from(grouped.entries()).map(([group, items]) => ({ group, items }))
})

// The success screen belongs to the billing flow, so it keeps the orders entry lit.
const activeMenu = computed(() => (route.path.startsWith('/billing') ? '/orders' : route.path))
</script>

<template>
  <div class="sidebar" :class="{ 'sidebar--collapsed': collapsed }">
    <div class="brand">
      <div class="brand-mark">wq</div>
      <div v-if="!collapsed" class="brand-copy">
        <span class="brand-name">saas-wq</span>
        <span class="brand-sub">Console</span>
      </div>
    </div>

    <el-scrollbar class="nav-scroll">
      <el-menu
        :default-active="activeMenu"
        :collapse="collapsed"
        :collapse-transition="false"
        router
        class="nav"
        background-color="transparent"
        text-color="#a8b4c7"
        active-text-color="#ffffff"
      >
        <el-menu-item-group v-for="section in sections" :key="section.group">
          <template #title>{{ section.group }}</template>
          <el-menu-item v-for="item in section.items" :key="item.path" :index="item.path">
            <el-icon><component :is="item.icon" /></el-icon>
            <template #title>{{ item.title }}</template>
          </el-menu-item>
        </el-menu-item-group>
      </el-menu>
    </el-scrollbar>

    <div v-if="!collapsed" class="sidebar-foot">
      <span class="dot" />
      <span>API healthy</span>
    </div>
  </div>
</template>

<style scoped>
.sidebar {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #0f172a;
  color: #ffffff;
  overflow: hidden;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  height: 64px;
  padding: 0 18px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  flex-shrink: 0;
}

.sidebar--collapsed .brand {
  justify-content: center;
  padding: 0;
}

.brand-mark {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 10px;
  background: linear-gradient(135deg, #2f6bf3, #4f8bff);
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 0.02em;
  flex-shrink: 0;
}

.brand-copy {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}

.brand-name {
  font-size: 15px;
  font-weight: 650;
  letter-spacing: -0.01em;
}

.brand-sub {
  font-size: 11px;
  color: #8b9bb4;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.nav-scroll {
  flex: 1;
  min-height: 0;
  padding: 12px 0;
}

.nav {
  border-right: none;
}

.nav :deep(.el-menu-item-group__title) {
  padding: 14px 22px 6px;
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: #64748b;
}

.sidebar--collapsed .nav :deep(.el-menu-item-group__title) {
  display: none;
}

.nav :deep(.el-menu-item) {
  height: 42px;
  margin: 2px 10px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
}

.nav :deep(.el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.07);
}

.nav :deep(.el-menu-item.is-active) {
  background: #2f6bf3;
  color: #ffffff;
}

.sidebar--collapsed .nav :deep(.el-menu-item) {
  margin: 2px 8px;
}

.sidebar-foot {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  font-size: 11px;
  color: #8b9bb4;
  flex-shrink: 0;
}

.dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #34d399;
  box-shadow: 0 0 0 3px rgba(52, 211, 153, 0.18);
}
</style>
