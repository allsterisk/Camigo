import type { ParcelStatus } from '@/types/parcel'

interface StatusDisplay {
  label: string
  type: 'primary' | 'success' | 'warning' | 'info' | 'danger'
  description: string
}

const statusMap: Record<ParcelStatus, StatusDisplay> = {
  ARRIVED: { label: '已到校', type: 'info', description: '包裹已进入校园配送流程' },
  DELIVERING: { label: '配送中', type: 'primary', description: '配送员正在将包裹送往目标位置' },
  STORED: { label: '待取件', type: 'warning', description: '包裹已存入目标快递柜' },
  PICKED: { label: '已取件', type: 'success', description: '包裹已从快递柜取出' },
  COMPLETED: { label: '已完成', type: 'success', description: '本次快递服务已经完成' },
}

const carrierMap: Record<string, string> = {
  SF: '顺丰速运',
  YTO: '圆通速递',
  ZTO: '中通快递',
  STO: '申通快递',
  YD: '韵达快递',
  JD: '京东物流',
  EMS: '中国邮政',
}

export function getStatusDisplay(status: string): StatusDisplay {
  return statusMap[status as ParcelStatus] ?? {
    label: status || '未知状态',
    type: 'info',
    description: '快递状态正在同步',
  }
}

export function formatCarrier(code: string): string {
  return carrierMap[code] ?? code
}

export function formatDateTime(value: string | null | undefined): string {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(date)
}

export function formatSize(sizeClass: string): string {
  return ({ SMALL: '小件', MEDIUM: '中件', LARGE: '大件' } as Record<string, string>)[sizeClass] ?? sizeClass
}
