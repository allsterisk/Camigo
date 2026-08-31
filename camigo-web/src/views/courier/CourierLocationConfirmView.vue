<script setup lang="ts">
import { onMounted } from 'vue'

import BrandMark from '@/components/brand/BrandMark.vue'
import CourierLocationForm from '@/components/courier/CourierLocationForm.vue'
import { useCourierLocation } from '@/composables/useCourierLocation'

const {
  locations,
  state,
  selectedLocationId,
  loading,
  submitting,
  errorMessage,
  load,
  confirm,
} = useCourierLocation()

onMounted(load)
</script>

<template>
  <main class="location-confirm-page">
    <section class="location-confirm-page__map" aria-label="山东大学软件园校区路线图">
      <div class="location-confirm-page__brand">
        <BrandMark :size="42" />
        <span><strong>校速达</strong><small>CAMIGO COURIER</small></span>
      </div>
      <img
        src="/images/camigo-campus-route-map.png"
        alt="山东大学软件园校区地点和道路距离示意图"
      />
      <p>你选择的地点将成为本次动态路线的起点。</p>
    </section>

    <section class="location-confirm-page__panel">
      <el-alert
        v-if="errorMessage"
        :title="errorMessage"
        type="error"
        :closable="false"
        show-icon
      />
      <CourierLocationForm
        v-model="selectedLocationId"
        :locations="locations"
        :state="state"
        :loading="loading"
        :submitting="submitting"
        @confirm="confirm"
      />
    </section>
  </main>
</template>

<style scoped>
.location-confirm-page { min-height: 100vh; display: grid; grid-template-columns: minmax(0, 1.15fr) minmax(420px, .85fr); background: #f2f7f5; }
.location-confirm-page__map { position: relative; display: flex; min-height: 100vh; flex-direction: column; justify-content: center; overflow: hidden; padding: 48px; background: #deeee7; }
.location-confirm-page__map::after { position: absolute; inset: 0; background: linear-gradient(180deg, rgb(9 75 69 / 4%), rgb(9 75 69 / 16%)); content: ''; pointer-events: none; }
.location-confirm-page__map img { position: relative; z-index: 1; width: 100%; border-radius: 16px; box-shadow: 0 24px 70px rgb(20 78 70 / 18%); }
.location-confirm-page__map p { position: relative; z-index: 1; margin: 18px 0 0; color: #3d665f; font-size: 13px; }
.location-confirm-page__brand { position: absolute; z-index: 2; top: 32px; left: 42px; display: flex; align-items: center; gap: 12px; color: #143e3a; }
.location-confirm-page__brand strong, .location-confirm-page__brand small { display: block; }
.location-confirm-page__brand strong { font-size: 18px; letter-spacing: .08em; }
.location-confirm-page__brand small { margin-top: 3px; color: #557770; font-size: 9px; letter-spacing: .18em; }
.location-confirm-page__panel { display: flex; min-height: 100vh; flex-direction: column; justify-content: center; gap: 22px; padding: clamp(36px, 7vw, 92px); background: #fff; }

@media (max-width: 900px) {
  .location-confirm-page { grid-template-columns: 1fr; }
  .location-confirm-page__map { min-height: 380px; padding: 94px 24px 30px; }
  .location-confirm-page__brand { top: 24px; left: 24px; }
  .location-confirm-page__panel { min-height: auto; padding: 44px 24px 56px; }
}
</style>
