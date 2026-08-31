import { computed, onScopeDispose, readonly, shallowRef } from 'vue'
import { acceptDispatchOffer, getCurrentDispatchOffers, rejectDispatchOffer } from '@/api/dispatch'
import type { DispatchOffer, DispatchRejectReason } from '@/types/dispatch'
import { getRequestErrorMessage } from '@/utils/request'

export function useCourierDispatchOffer() {
  const offers = shallowRef<DispatchOffer[]>([])
  const loading = shallowRef(false)
  const submittingAssignmentId = shallowRef<number | null>(null)
  const errorMessage = shallowRef('')
  const now = shallowRef(Date.now())
  let loadVersion = 0
  let expiryRefreshScheduled = false

  const remainingSecondsById = computed<Record<number, number>>(() => Object.fromEntries(
    offers.value.map((offer) => [
      offer.assignmentId,
      Math.max(0, Math.ceil((new Date(offer.offerExpireTime).getTime() - now.value) / 1000)),
    ]),
  ))

  const timer = window.setInterval(() => {
    now.value = Date.now()
    if (!expiryRefreshScheduled && offers.value.some(
      (offer) => remainingSecondsById.value[offer.assignmentId] === 0,
    )) {
      expiryRefreshScheduled = true
      window.setTimeout(() => void load(), 800)
    }
  }, 1000)
  onScopeDispose(() => window.clearInterval(timer))

  async function load() {
    const version = ++loadVersion
    loading.value = true
    errorMessage.value = ''
    try {
      const result = await getCurrentDispatchOffers()
      if (version !== loadVersion) return
      offers.value = result
      expiryRefreshScheduled = false
      now.value = Date.now()
    } catch (error) {
      if (version !== loadVersion) return
      offers.value = []
      errorMessage.value = getRequestErrorMessage(error, '派单邀请加载失败')
    } finally {
      if (version === loadVersion) loading.value = false
    }
  }

  async function accept(assignmentId: number): Promise<boolean> {
    if (submittingAssignmentId.value !== null
      || remainingSecondsById.value[assignmentId] === 0) return false
    submittingAssignmentId.value = assignmentId
    errorMessage.value = ''
    try {
      await acceptDispatchOffer(assignmentId)
      offers.value = offers.value.filter((offer) => offer.assignmentId !== assignmentId)
      return true
    } catch (error) {
      errorMessage.value = getRequestErrorMessage(error, '邀请已失效或无法接受，请刷新后重试')
      await load()
      return false
    } finally {
      submittingAssignmentId.value = null
    }
  }

  async function reject(assignmentId: number, reasonCode: DispatchRejectReason,
                        reasonDetail?: string): Promise<boolean> {
    if (submittingAssignmentId.value !== null
      || remainingSecondsById.value[assignmentId] === 0) return false
    submittingAssignmentId.value = assignmentId
    errorMessage.value = ''
    try {
      await rejectDispatchOffer(assignmentId, { reasonCode, reasonDetail })
      offers.value = offers.value.filter((offer) => offer.assignmentId !== assignmentId)
      return true
    } catch (error) {
      errorMessage.value = getRequestErrorMessage(error, '邀请拒绝失败，请刷新后重试')
      await load()
      return false
    } finally {
      submittingAssignmentId.value = null
    }
  }

  return {
    offers: readonly(offers), loading: readonly(loading),
    submittingAssignmentId: readonly(submittingAssignmentId),
    errorMessage: readonly(errorMessage), remainingSecondsById, load, accept, reject,
  }
}
