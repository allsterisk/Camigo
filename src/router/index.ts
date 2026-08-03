import { createRouter, createWebHistory, type RouteLocationNormalized } from 'vue-router'

import { pinia } from '@/store'
import { useUserStore, type UserRole } from '@/store/user'

const roleHome: Record<UserRole, string> = {
  USER: '/user',
  COURIER: '/courier',
  ADMIN: '/admin',
}

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', redirect: '/login' },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginPlaceholderView.vue'),
      meta: { public: true, title: '登录' },
    },
    ...(['USER', 'COURIER', 'ADMIN'] as const).map((role) => ({
      path: roleHome[role],
      name: role.toLowerCase(),
      component: () => import('@/views/RolePlaceholderView.vue'),
      props: { role },
      meta: { requiresAuth: true, roles: [role], title: roleHome[role].slice(1) },
    })),
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: () => import('@/views/NotFoundView.vue'),
      meta: { public: true, title: '页面不存在' },
    },
  ],
})

router.beforeEach((to) => {
  const userStore = useUserStore(pinia)

  if (to.meta.public) {
    if (to.name === 'login' && userStore.isAuthenticated && userStore.role) {
      return roleHome[userStore.role]
    }
    return true
  }

  if (!userStore.isAuthenticated || !userStore.role) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  if (!canAccess(to, userStore.role)) return roleHome[userStore.role]
  return true
})

router.afterEach((to) => {
  document.title = `${String(to.meta.title ?? 'Camigo')} · 校速达`
})

function canAccess(to: RouteLocationNormalized, role: UserRole): boolean {
  return !to.meta.roles || to.meta.roles.includes(role)
}

export default router
