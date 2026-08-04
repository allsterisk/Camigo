import { readonly, shallowRef } from 'vue'
import { deleteAdminUser, getAdminUsers, updateAdminUserStatus } from '@/api/admin'
import type { AdminUser, AdminUserQuery, UserStatus } from '@/types/admin'
import { getRequestErrorMessage } from '@/utils/request'

export function useAdminUsers() {
  const records = shallowRef<AdminUser[]>([])
  const total = shallowRef(0)
  const loading = shallowRef(false)
  const actionId = shallowRef<number | null>(null)
  const errorMessage = shallowRef('')

  async function load(query: AdminUserQuery) {
    loading.value = true
    errorMessage.value = ''
    try {
      const result = await getAdminUsers(query)
      records.value = result.records
      total.value = result.total
    } catch (error) {
      records.value = []
      total.value = 0
      errorMessage.value = getRequestErrorMessage(error, '用户列表加载失败')
    } finally { loading.value = false }
  }

  async function changeStatus(id: number, status: UserStatus) {
    return runAction(id, () => updateAdminUserStatus(id, status), '用户状态更新失败')
  }

  async function remove(id: number) {
    return runAction(id, () => deleteAdminUser(id), '用户删除失败')
  }

  async function runAction(id: number, action: () => Promise<unknown>, fallback: string) {
    if (actionId.value !== null) return false
    actionId.value = id
    try { await action(); return true }
    catch (error) { errorMessage.value = getRequestErrorMessage(error, fallback); return false }
    finally { actionId.value = null }
  }

  return { records: readonly(records), total: readonly(total), loading: readonly(loading),
    actionId: readonly(actionId), errorMessage: readonly(errorMessage), load, changeStatus, remove }
}
