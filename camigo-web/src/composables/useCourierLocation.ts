import { readonly, ref, shallowRef } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import {
  confirmCourierLocation,
  getCampusLocationOptions,
  getCourierLocationState,
} from '@/api/courier-execution'
import { useUserStore } from '@/store/user'
import type { CampusLocationOption, CourierLocationState } from '@/types/courier-execution'
import { getRequestErrorMessage } from '@/utils/request'

export function useCourierLocation() {
  const route = useRoute()
  const router = useRouter()
  const userStore = useUserStore()
  const locations = ref<CampusLocationOption[]>([])
  const state = ref<CourierLocationState | null>(null)
  const selectedLocationId = shallowRef<number | null>(null)
  const loading = shallowRef(false)
  const submitting = shallowRef(false)
  const errorMessage = shallowRef('')

  async function load() {
    if (loading.value) return
    loading.value = true
    errorMessage.value = ''
    try {
      const [options, current] = await Promise.all([
        getCampusLocationOptions(),
        getCourierLocationState(),
      ])
      locations.value = options
      state.value = current
      selectedLocationId.value = current.currentLocationId
    } catch (error) {
      errorMessage.value = getRequestErrorMessage(error, '校园地点加载失败')
    } finally {
      loading.value = false
    }
  }

  async function confirm(): Promise<boolean> {
    if (submitting.value) return false
    if (!selectedLocationId.value) {
      errorMessage.value = '请选择你当前所在的校园地点'
      return false
    }
    submitting.value = true
    errorMessage.value = ''
    try {
      state.value = await confirmCourierLocation(selectedLocationId.value)
      userStore.markLocationConfirmed()
      await router.replace(resolveDestination())
      return true
    } catch (error) {
      errorMessage.value = getRequestErrorMessage(error, '当前位置确认失败')
      return false
    } finally {
      submitting.value = false
    }
  }

  function resolveDestination(): string {
    const redirect = route.query.redirect
    if (typeof redirect !== 'string' || !redirect.startsWith('/') || redirect.startsWith('//')) {
      return '/courier'
    }
    const resolved = router.resolve(redirect)
    if (
      resolved.matched.length === 0
      || resolved.name === 'courier-location-confirm'
      || (resolved.meta.roles && !resolved.meta.roles.includes('COURIER'))
    ) {
      return '/courier'
    }
    return redirect
  }

  return {
    locations: readonly(locations),
    state: readonly(state),
    selectedLocationId,
    loading: readonly(loading),
    submitting: readonly(submitting),
    errorMessage: readonly(errorMessage),
    load,
    confirm,
  }
}
