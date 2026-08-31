import { readonly, shallowRef, watch, type Ref } from 'vue'

import { getParcelPickupAccess, pickupParcel } from '@/api/parcel'
import type { ParcelPickupAccess, PickupResult } from '@/types/locker'
import type { Parcel } from '@/types/parcel'
import { getRequestErrorMessage } from '@/utils/request'

export function useParcelPickup(parcel: Readonly<Ref<Parcel | null>>) {
  const access = shallowRef<ParcelPickupAccess | null>(null)
  const loading = shallowRef(false)
  const submitting = shallowRef(false)
  const errorMessage = shallowRef('')
  const pickupResult = shallowRef<PickupResult | null>(null)
  let requestVersion = 0

  async function load(parcelId: number) {
    const version = ++requestVersion
    loading.value = true
    errorMessage.value = ''
    try {
      const result = await getParcelPickupAccess(parcelId)
      if (version === requestVersion) access.value = result
    } catch (error) {
      if (version === requestVersion) {
        access.value = null
        errorMessage.value = getRequestErrorMessage(error, '取件信息加载失败，请刷新重试')
      }
    } finally {
      if (version === requestVersion) loading.value = false
    }
  }

  async function pickup(code: string): Promise<boolean> {
    const parcelId = parcel.value?.id
    if (!parcelId || submitting.value || !/^\d{4}$/.test(code)) return false
    submitting.value = true
    errorMessage.value = ''
    try {
      pickupResult.value = await pickupParcel(parcelId, code)
      access.value = null
      return true
    } catch (error) {
      errorMessage.value = getRequestErrorMessage(error, '取件失败，请核对四位取件码')
      return false
    } finally {
      submitting.value = false
    }
  }

  watch(
    () => [parcel.value?.id, parcel.value?.currentStatus] as const,
    ([parcelId, status], previous) => {
      if (previous?.[0] !== parcelId) pickupResult.value = null
      requestVersion++
      access.value = null
      loading.value = false
      errorMessage.value = ''
      if (parcelId && status === 'STORED') void load(parcelId)
    },
    { immediate: true },
  )

  return {
    access: readonly(access),
    loading: readonly(loading),
    submitting: readonly(submitting),
    errorMessage: readonly(errorMessage),
    pickupResult: readonly(pickupResult),
    pickup,
  }
}
