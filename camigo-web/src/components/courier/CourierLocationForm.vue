<script setup lang="ts">
import { Location } from '@element-plus/icons-vue'

import type { CampusLocationOption, CourierLocationState } from '@/types/courier-execution'

const selectedLocationId = defineModel<number | null>({ required: true })

defineProps<{
  locations: readonly CampusLocationOption[]
  state: Readonly<CourierLocationState> | null
  loading: boolean
  submitting: boolean
}>()

defineEmits<{ confirm: [] }>()
</script>

<template>
  <section class="location-form" aria-labelledby="location-title">
    <div class="location-form__icon" aria-hidden="true">
      <el-icon><Location /></el-icon>
    </div>
    <div class="location-form__copy">
      <h1 id="location-title">从哪里开始配送？</h1>
      <p>确认后，系统会以这里作为路线起点，并恢复你的接单状态。</p>
    </div>

    <el-alert
      v-if="state?.currentLocationName"
      :title="`上次记录：${state.currentLocationName}`"
      description="本次登录仍需重新确认，避免使用过期位置计算路线。"
      type="info"
      :closable="false"
      show-icon
    />

    <el-select
      v-model="selectedLocationId"
      class="location-form__select"
      filterable
      :loading="loading"
      placeholder="选择当前校园地点"
      size="large"
      aria-label="当前校园地点"
    >
      <el-option
        v-for="location in locations"
        :key="location.id"
        :label="location.locationName"
        :value="location.id"
      />
    </el-select>

    <el-button
      class="location-form__submit"
      type="primary"
      size="large"
      :loading="submitting"
      :disabled="loading || !selectedLocationId"
      @click="$emit('confirm')"
    >
      确认位置并进入工作台
    </el-button>
  </section>
</template>

<style scoped>
.location-form { display: flex; flex-direction: column; gap: 22px; }
.location-form__icon { display: grid; width: 54px; height: 54px; place-items: center; border-radius: 15px; color: #fff; background: #087f73; box-shadow: 0 12px 28px rgb(8 127 115 / 22%); font-size: 25px; }
.location-form__copy h1 { margin: 0; color: #173f3b; font-size: clamp(28px, 4vw, 40px); letter-spacing: -.025em; line-height: 1.15; }
.location-form__copy p { max-width: 48ch; margin: 12px 0 0; color: #557572; font-size: 15px; line-height: 1.75; }
.location-form__select { width: 100%; }
.location-form__submit { width: 100%; min-height: 48px; font-weight: 700; }
</style>
