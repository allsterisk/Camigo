import { shallowRef } from 'vue'
import { generateAdminParcels, getParcelGenerationOptions } from '@/api/admin'
import type { GeneratedParcel, ParcelGenerationOptions, ParcelGenerationRequest } from '@/types/parcel-generation'
import { getRequestErrorMessage } from '@/utils/request'

export function useAdminParcelGeneration() {
  const options = shallowRef<ParcelGenerationOptions | null>(null)
  const generated = shallowRef<GeneratedParcel[]>([])
  const loading = shallowRef(false)
  const submitting = shallowRef(false)
  const errorMessage = shallowRef('')

  async function loadOptions() {
    if (loading.value) return
    loading.value = true
    errorMessage.value = ''
    try {
      options.value = await getParcelGenerationOptions()
    } catch (error) {
      errorMessage.value = getRequestErrorMessage(error, '生成选项加载失败')
    } finally {
      loading.value = false
    }
  }

  async function generate(payload: ParcelGenerationRequest): Promise<boolean> {
    if (submitting.value) return false
    submitting.value = true
    errorMessage.value = ''
    try {
      generated.value = await generateAdminParcels(payload)
      return true
    } catch (error) {
      errorMessage.value = getRequestErrorMessage(error, '包裹生成失败，当前批次未写入')
      return false
    } finally {
      submitting.value = false
    }
  }

  return { options, generated, loading, submitting, errorMessage, loadOptions, generate }
}
