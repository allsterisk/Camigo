import { shallowRef } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter, type RouteLocationRaw } from 'vue-router'

import { useUserStore } from '@/store/user'
import { ROLE_HOME, type LoginCredentials, type UserRole } from '@/types/auth'
import { getRequestErrorMessage } from '@/utils/request'

export function useLogin() {
  const route = useRoute()
  const router = useRouter()
  const userStore = useUserStore()
  const isSubmitting = shallowRef(false)
  const errorMessage = shallowRef('')

  async function submit(credentials: LoginCredentials) {
    if (isSubmitting.value) return
    isSubmitting.value = true
    errorMessage.value = ''

    try {
      const role = await userStore.login(credentials)
      await router.replace(resolveDestination(role))
      ElMessage.success(`欢迎回来，${userStore.displayName}`)
    } catch (error) {
      errorMessage.value = getRequestErrorMessage(error, '登录失败，请检查账号和密码')
    } finally {
      isSubmitting.value = false
    }
  }

  function resolveDestination(role: UserRole): RouteLocationRaw {
    const redirect = route.query.redirect
    if (role === 'COURIER' && !userStore.locationConfirmed) {
      return {
        name: 'courier-location-confirm',
        query: typeof redirect === 'string' ? { redirect } : undefined,
      }
    }
    if (typeof redirect !== 'string' || !redirect.startsWith('/') || redirect.startsWith('//')) {
      return ROLE_HOME[role]
    }

    const resolved = router.resolve(redirect)
    if (resolved.matched.length === 0 || (resolved.meta.roles && !resolved.meta.roles.includes(role))) {
      return ROLE_HOME[role]
    }
    return redirect
  }

  return { isSubmitting, errorMessage, submit }
}
