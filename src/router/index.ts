import { createRouter, createWebHistory, type RouteLocationNormalized } from 'vue-router'

import { pinia } from '@/store'
import { useUserStore } from '@/store/user'
import { ROLE_HOME, type UserRole } from '@/types/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', redirect: '/login' },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { public: true, guestOnly: true, title: '登录' },
    },
    ...(['USER', 'COURIER', 'ADMIN'] as const).map((role) => ({
      path: ROLE_HOME[role],
      name: role.toLowerCase(),
      component: () => import('@/views/RolePlaceholderView.vue'),
      props: { role },
      meta: { requiresAuth: true, roles: [role], title: ROLE_HOME[role].slice(1) },
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

  if (to.meta.guestOnly && userStore.isAuthenticated && userStore.role) {
    return ROLE_HOME[userStore.role]
  }

  if (to.meta.requiresAuth && (!userStore.isAuthenticated || !userStore.role)) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  if (userStore.role && !canAccess(to, userStore.role)) return ROLE_HOME[userStore.role]
  return true
})

router.afterEach((to) => {
  document.title = `${String(to.meta.title ?? 'Camigo')} · 校速达`
})

function canAccess(to: RouteLocationNormalized, role: UserRole): boolean {
  return !to.meta.roles || to.meta.roles.includes(role)
}

export default router
