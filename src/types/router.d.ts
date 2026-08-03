import 'vue-router'
import type { UserRole } from '@/store/user'

export {}

declare module 'vue-router' {
  interface RouteMeta {
    public?: boolean
    requiresAuth?: boolean
    roles?: UserRole[]
    title?: string
  }
}
