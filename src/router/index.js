import { createRouter, createWebHistory } from 'vue-router'
import { readSession } from '../utils/session'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/LoginView.vue'),
      meta: { public: true, title: '登录' },
    },
    {
      path: '/',
      component: () => import('../layouts/AppLayout.vue'),
      children: [
        {
          path: '',
          name: 'home',
          component: () => import('../views/HomeView.vue'),
          meta: { title: '工作台' },
        },
        {
          path: 'parcels',
          name: 'my-parcels',
          component: () => import('../views/MyParcelsView.vue'),
          meta: { title: '我的快递', roles: ['USER'] },
        },
        {
          path: 'delivery-tasks',
          name: 'delivery-tasks',
          component: () => import('../views/CourierTasksView.vue'),
          meta: { title: '配送任务', roles: ['COURIER'] },
        },
      ],
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: () => import('../views/NotFoundView.vue'),
      meta: { public: true, title: '页面不存在' },
    },
  ],
})

router.beforeEach((to) => {
  const session = readSession()
  document.title = `${to.meta.title || 'Camigo'} · 校速达`

  if (!to.meta.public && !session?.accessToken) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.name === 'login' && session?.accessToken) {
    return { name: 'home' }
  }
  if (to.meta.roles?.length) {
    const roles = session?.user?.roles || []
    if (!to.meta.roles.some((role) => roles.includes(role))) {
      return { name: 'home' }
    }
  }
  return true
})

export default router
