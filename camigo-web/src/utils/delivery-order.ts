import type {
  CourierDeliveryOrder,
  DeliveryOrderPriority,
  DeliveryOrderStatus,
} from '@/types/delivery-order'

interface DeliveryStatusDisplay {
  label: string
  type: 'primary' | 'success' | 'warning' | 'info'
}

const statusMap: Record<DeliveryOrderStatus, DeliveryStatusDisplay> = {
  WAITING: { label: '待接单', type: 'warning' },
  PROCESSING: { label: '配送中', type: 'primary' },
  DELIVERED: { label: '已送达', type: 'success' },
}

const priorityMap: Record<DeliveryOrderPriority, string> = {
  LOW: '低',
  NORMAL: '普通',
  HIGH: '高',
  URGENT: '紧急',
}

export function getDeliveryStatusDisplay(status: string): DeliveryStatusDisplay {
  return statusMap[status as DeliveryOrderStatus] ?? { label: status || '未知', type: 'info' }
}

export function formatDeliveryPriority(priority: string): string {
  return priorityMap[priority as DeliveryOrderPriority] ?? priority
}

export function getDeliveryOrderTime(order: CourierDeliveryOrder): string {
  return order.completedTime || order.acceptedTime || order.createTime
}

export function isToday(value: string | null | undefined): boolean {
  if (!value) return false
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return false
  const today = new Date()
  return date.getFullYear() === today.getFullYear()
    && date.getMonth() === today.getMonth()
    && date.getDate() === today.getDate()
}
