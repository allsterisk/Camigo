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
    {
      path: '/user',
      component: () => import('@/layout/UserLayout.vue'),
      meta: { requiresAuth: true, roles: ['USER'] },
      children: [
        {
          path: '',
          name: 'user-dashboard',
          component: () => import('@/views/user/UserDashboardView.vue'),
          meta: { title: '学生首页' },
        },
        {
          path: 'parcels',
          name: 'user-parcels',
          component: () => import('@/views/user/UserParcelsView.vue'),
          meta: { title: '我的快递' },
        },
        {
          path: 'parcels/:id',
          name: 'user-parcel-detail',
          component: () => import('@/views/user/UserParcelDetailView.vue'),
          meta: { title: '快递详情' },
        },
        {
          path: 'evaluation',
          name: 'user-evaluation',
          component: () => import('@/views/user/UserEvaluationView.vue'),
          meta: { title: '配送评价' },
        },
      ],
    },
    {
      path: '/courier',
      component: () => import('@/layout/CourierLayout.vue'),
      meta: { requiresAuth: true, roles: ['COURIER'] },
      children: [
        {
          path: '',
          name: 'courier-dashboard',
          component: () => import('@/views/courier/CourierDashboardView.vue'),
          meta: { title: '配送首页' },
        },
        {
          path: 'tasks',
          name: 'courier-tasks',
          component: () => import('@/views/courier/CourierTasksView.vue'),
          meta: { title: '我的任务' },
        },
        {
          path: 'history',
          name: 'courier-history',
          component: () => import('@/views/courier/CourierHistoryView.vue'),
          meta: { title: '配送记录' },
        },
      ],
    },
    {
      path: '/admin',
      component: () => import('@/layout/AdminLayout.vue'),
      meta: { requiresAuth: true, roles: ['ADMIN'] },
      children: [
        { path: '', name: 'admin-dashboard', component: () => import('@/views/admin/AdminDashboardView.vue'), meta: { title: '运营概览' } },
        { path: 'users', name: 'admin-users', component: () => import('@/views/admin/AdminUsersView.vue'), meta: { title: '用户管理' } },
        { path: 'parcels', name: 'admin-parcels', component: () => import('@/views/admin/AdminParcelsView.vue'), meta: { title: '快递管理' } },
        { path: 'couriers', name: 'admin-couriers', component: () => import('@/views/admin/AdminCouriersView.vue'), meta: { title: '配送员管理' } },
        { path: 'lockers', name: 'admin-lockers', component: () => import('@/views/admin/AdminLockersView.vue'), meta: { title: '快递柜管理' } },
      ],
    },
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
