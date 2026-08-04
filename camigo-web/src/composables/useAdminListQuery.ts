import { computed, shallowRef, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

export function useAdminListQuery(statusKey: string) {
  const route = useRoute()
  const router = useRouter()
  const keyword = shallowRef('')
  const status = shallowRef('')
  const page = computed(() => parsePage(route.query.page))

  watch(() => route.query, (query) => {
    keyword.value = single(query.keyword)
    status.value = single(query[statusKey])
  }, { immediate: true })

  function search() {
    return router.replace({ query: {
      keyword: keyword.value.trim() || undefined,
      [statusKey]: status.value || undefined,
      page: undefined,
    } })
  }
  function reset() { keyword.value = ''; status.value = ''; return search() }
  function setPage(value: number) {
    return router.replace({ query: { ...route.query, page: value === 1 ? undefined : String(value) } })
  }
  return { route, keyword, status, page, search, reset, setPage }
}

function single(value: unknown): string {
  const raw = Array.isArray(value) ? value[0] : value
  return typeof raw === 'string' ? raw : ''
}

function parsePage(value: unknown): number {
  const parsed = Number.parseInt(single(value) || '1', 10)
  return Number.isInteger(parsed) && parsed > 0 ? parsed : 1
}
