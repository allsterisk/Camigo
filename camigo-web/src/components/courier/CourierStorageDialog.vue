<script setup lang="ts">
import { computed, shallowRef, watch } from 'vue'

import type { LockerSlotOption } from '@/types/locker'

const props = defineProps<{
  slots: readonly LockerSlotOption[]
  loading: boolean
  submitting: boolean
  errorMessage?: string
}>()

const visible = defineModel<boolean>({ required: true })
const emit = defineEmits<{
  confirm: [lockerSlotId: number]
  closed: []
}>()
const selectedSlotId = shallowRef<number | null>(null)
const canSubmit = computed(() => selectedSlotId.value !== null && !props.submitting)

watch(visible, (isVisible) => {
  if (isVisible) selectedSlotId.value = null
})

function close() {
  if (props.submitting) return
  visible.value = false
  emit('closed')
}
</script>

<template>
  <el-dialog
    v-model="visible"
    class="storage-dialog"
    title="选择入柜格口"
    width="min(620px, calc(100vw - 32px))"
    :close-on-click-modal="!submitting"
    :close-on-press-escape="!submitting"
    :show-close="!submitting"
    @closed="emit('closed')"
  >
    <p class="storage-dialog__hint">
      仅显示目标快递柜中当前空闲且能容纳该包裹的格口。提交后将生成学生专属四位取件码。
    </p>
    <el-alert
      v-if="errorMessage"
      class="storage-dialog__alert"
      :title="errorMessage"
      type="error"
      :closable="false"
      show-icon
    />
    <div v-loading="loading" class="storage-dialog__content">
      <el-radio-group v-if="slots.length" v-model="selectedSlotId" class="storage-dialog__slots">
        <el-radio-button v-for="slot in slots" :key="slot.id" :value="slot.id">
          <span class="storage-dialog__slot-no">{{ slot.slotNo }}</span>
          <small>{{ slot.sizeClass }} 型格口</small>
        </el-radio-button>
      </el-radio-group>
      <div v-else-if="!loading" class="storage-dialog__empty" role="status">
        <strong>暂无兼容空闲格口</strong>
        <p>可能有其他配送员刚刚占用格口，请关闭后刷新任务再试。</p>
      </div>
    </div>
    <template #footer>
      <el-button :disabled="submitting" @click="close">取消</el-button>
      <el-button
        type="primary"
        :disabled="!canSubmit"
        :loading="submitting"
        @click="selectedSlotId !== null && emit('confirm', selectedSlotId)"
      >确认入柜</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.storage-dialog__hint { max-width: 58ch; margin: 0 0 18px; color: #607d79; font-size: 14px; line-height: 1.7; }
.storage-dialog__alert { margin-bottom: 16px; }
.storage-dialog__content { min-height: 150px; }
.storage-dialog__slots { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 10px; width: 100%; }
.storage-dialog__slots :deep(.el-radio-button) { min-width: 0; }
.storage-dialog__slots :deep(.el-radio-button__inner) { width: 100%; min-height: 68px; display: flex; flex-direction: column; align-items: flex-start; justify-content: center; gap: 5px; border: 1px solid #d9e8e5; border-radius: 12px; box-shadow: none; }
.storage-dialog__slots :deep(.el-radio-button:first-child .el-radio-button__inner),
.storage-dialog__slots :deep(.el-radio-button:last-child .el-radio-button__inner) { border-radius: 12px; }
.storage-dialog__slot-no { overflow: hidden; max-width: 100%; color: #214f49; font-weight: 700; text-overflow: ellipsis; white-space: nowrap; }
.storage-dialog__slots small { color: #78918d; font-size: 11px; }
.storage-dialog__empty { display: grid; min-height: 150px; place-content: center; color: #6f8985; text-align: center; }
.storage-dialog__empty strong { color: #365f59; }
.storage-dialog__empty p { max-width: 38ch; margin: 8px auto 0; font-size: 13px; line-height: 1.6; }

@media (max-width: 560px) {
  .storage-dialog__slots { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
</style>
