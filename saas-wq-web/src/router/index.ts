import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { Role } from '@/api/types'
import { readToken } from '@/utils/session'

declare module 'vue-router' {
  interface RouteMeta {
    /** Pages reachable without a session, currently only the login screen. */
    public?: boolean
    /** Document title suffix and breadcrumb label. */
    title?: string
    /** Sidebar icon registered globally from @element-plus/icons-vue. */
    icon?: string
    /** Sidebar grouping label. */
    group?: string
    /** Roles allowed to open the page. */
    roles?: Role[]
  }
}

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/login/LoginView.vue'),
    meta: { public: true, title: 'Sign in' }
  },
  {
    path: '/',
    component: () => import('@/layout/AppLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('@/views/dashboard/DashboardView.vue'),
        meta: { title: 'Dashboard', icon: 'Odometer', group: 'Overview' }
      },
      {
        path: 'tenants',
        name: 'tenants',
        component: () => import('@/views/tenant/TenantListView.vue'),
        meta: { title: 'Tenants', icon: 'OfficeBuilding', group: 'Platform', roles: ['SUPER_ADMIN'] }
      },
      {
        path: 'customers',
        name: 'customers',
        component: () => import('@/views/customer/CustomerListView.vue'),
        meta: { title: 'Customers', icon: 'UserFilled', group: 'Business' }
      },
      {
        path: 'orders',
        name: 'orders',
        component: () => import('@/views/order/OrderListView.vue'),
        meta: { title: 'Orders & Billing', icon: 'CreditCard', group: 'Business' }
      },
      {
        path: 'users',
        name: 'users',
        component: () => import('@/views/user/UserListView.vue'),
        meta: {
          title: 'Users',
          icon: 'User',
          group: 'Administration',
          roles: ['SUPER_ADMIN', 'TENANT_ADMIN']
        }
      },
      {
        path: 'rbac',
        name: 'rbac',
        component: () => import('@/views/rbac/RbacView.vue'),
        meta: {
          title: 'Roles & permissions',
          icon: 'Lock',
          group: 'Administration',
          roles: ['SUPER_ADMIN', 'TENANT_ADMIN']
        }
      },
      {
        path: 'audit-logs',
        name: 'audit-logs',
        component: () => import('@/views/audit/AuditLogView.vue'),
        meta: {
          title: 'Audit trail',
          icon: 'Document',
          group: 'Administration',
          roles: ['SUPER_ADMIN', 'TENANT_ADMIN']
        }
      },
      {
        path: 'billing/success',
        name: 'billing-success',
        component: () => import('@/views/billing/BillingSuccessView.vue'),
        meta: { title: 'Payment complete', group: 'Business' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/views/error/NotFoundView.vue'),
    meta: { public: true, title: 'Page not found' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

router.beforeEach(async (to) => {
  // Imported lazily so the API client can own the router without a static cycle.
  const { useAuthStore } = await import('@/stores/auth')
  const auth = useAuthStore()
  const token = readToken()

  if (!token) {
    auth.reset()
  }

  if (to.meta.public) {
    if (to.name === 'login' && token) {
      return { name: 'dashboard' }
    }
    return true
  }

  if (!token) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  const allowed = to.meta.roles
  if (allowed && auth.role && !allowed.includes(auth.role)) {
    ElMessage.warning('You do not have access to this page.')
    return { name: 'dashboard' }
  }

  return true
})

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} · saas-wq` : 'saas-wq Console'
})

export default router
