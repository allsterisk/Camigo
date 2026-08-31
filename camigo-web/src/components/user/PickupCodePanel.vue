<script setup lang="ts">
import { computed, shallowRef, watch } from 'vue'
import { Key, Location } from '@element-plus/icons-vue'

import type { ParcelPickupAccess } from '@/types/locker'

const props = defineProps<{
  access: Readonly<ParcelPickupAccess> | null
  loading: boolean
  submitting: boolean
  errorMessage?: string
}>()
const emit = defineEmits<{ pickup: [pickupCode: string] }>()
const inputCode = shallowRef('')
const canPickup = computed(() => /^\d{4}$/.test(inputCode.value) && !props.submitting)

watch(() => props.access?.storageRecordId, () => {
  inputCode.value = ''
})

function updateCode(value: string) {
  inputCode.value = value.replace(/\D/g, '').slice(0, 4)
}
</script>

<template>
  <section class="pickup-panel" aria-labelledby="pickup-panel-title" v-loading="loading">
    <div class="pickup-panel__heading">
      <div>
        <h2 id="pickup-panel-title">快递柜取件</h2>
        <p>四位取件码仅在你的快递详情中显示，取件成功后立即失效。</p>
      </div>
      <el-icon class="pickup-panel__key" aria-hidden="true"><Key /></el-icon>
    </div>

    <el-alert
      v-if="errorMessage"
      class="pickup-panel__alert"
      :title="errorMessage"
      type="error"
      :closable="false"
      show-icon
    />

    <template v-if="access">
      <div class="pickup-panel__location">
        <el-icon aria-hidden="true"><Location /></el-icon>
        <span><strong>{{ access.lockerName }}</strong> · 格口 {{ access.slotNo }}</span>
      </div>
      <div v-if="access.pickupCode" class="pickup-panel__code" aria-live="polite">
        <span>你的取件码</span>
        <strong>{{ access.pickupCode }}</strong>
      </div>
      <el-alert
        v-else
        title="这是迁移前的历史入柜记录，请使用原先收到的取件码。"
        type="warning"
        :closable="false"
        show-icon
      />
      <div class="pickup-panel__form">
        <el-input
          :model-value="inputCode"
          inputmode="numeric"
          autocomplete="one-time-code"
          maxlength="4"
          size="large"
          placeholder="输入四位取件码"
          aria-label="四位取件码"
          @update:model-value="updateCode"
          @keyup.enter="canPickup && emit('pickup', inputCode)"
        />
        <el-button
          type="primary"
          size="large"
          :disabled="!canPickup"
          :loading="submitting"
          @click="emit('pickup', inputCode)"
        >确认取件</el-button>
      </div>
    </template>

    <div v-else-if="!loading && !errorMessage" class="pickup-panel__empty" role="status">
      包裹尚未生成可用取件码。
    </div>
  </section>
</template>

<style scoped>
.pickup-panel { padding: 26px; border-radius: 16px; background: #fff; box-shadow: 0 12px 32px rgb(15 75 70 / 8%); }
.pickup-panel__heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 20px; margin-bottom: 20px; }
.pickup-panel__heading h2 { margin: 0; color: #214f49; font-size: 20px; letter-spacing: -.015em; }
.pickup-panel__heading p { max-width: 58ch; margin: 7px 0 0; color: #69847f; font-size: 13px; line-height: 1.65; }
.pickup-panel__key { flex: 0 0 42px; width: 42px; height: 42px; border-radius: 13px; color: #087f73; background: #e8f5f2; font-size: 20px; }
.pickup-panel__alert { margin-bottom: 16px; }
.pickup-panel__location { display: flex; align-items: center; gap: 9px; color: #54736e; font-size: 14px; }
.pickup-panel__location strong { color: #2b5751; }
.pickup-panel__code { display: flex; align-items: end; justify-content: space-between; gap: 24px; margin: 18px 0; padding: 20px 22px; border-radius: 14px; background: #eff8f5; }
.pickup-panel__code span { color: #64817c; font-size: 12px; }
.pickup-panel__code strong { color: #086f66; font-family: ui-monospace, SFMono-Regular, Consolas, monospace; font-size: clamp(34px, 8vw, 50px); letter-spacing: .12em; line-height: 1; }
.pickup-panel__form { display: grid; grid-template-columns: minmax(0, 1fr) auto; gap: 12px; margin-top: 18px; }
.pickup-panel__form :deep(.el-input__inner) { font-size: 18px; letter-spacing: .12em; }
.pickup-panel__empty { min-height: 90px; display: grid; place-content: center; color: #718b86; font-size: 14px; }

@media (max-width: 520px) {
  .pickup-panel { padding: 20px; }
  .pickup-panel__code { align-items: flex-start; flex-direction: column; gap: 10px; }
  .pickup-panel__form { grid-template-columns: 1fr; }
}
</style>
