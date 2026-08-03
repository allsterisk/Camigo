import 'vue-router'
import type { UserRole } from '@/types/auth'

export {}

declare module 'vue-router' {
  interface RouteMeta {
    public?: boolean
    guestOnly?: boolean
    requiresAuth?: boolean
    roles?: UserRole[]
    title?: string
  }
}
