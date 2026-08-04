<script setup lang="ts">
import BrandMark from '@/components/brand/BrandMark.vue'

const roleItems = [
  { code: 'USER', label: '学生用户', description: '查询快递与取件进度' },
  { code: 'COURIER', label: '配送员', description: '接收并处理配送任务' },
  { code: 'ADMIN', label: '管理员', description: '管理校园物流运营' },
] as const
</script>

<template>
  <section class="brand-panel">
    <div class="brand-panel__glow" aria-hidden="true" />
    <div class="brand-panel__identity">
      <BrandMark :size="72" />
      <div>
        <strong class="brand-panel__name">校速达</strong>
        <span class="brand-panel__english">CAMIGO</span>
      </div>
    </div>

    <div class="brand-panel__content">
      <p class="brand-panel__eyebrow">校园智能物流平台</p>
      <h1 class="brand-panel__title">让每一件快递，<br />更快抵达校园生活。</h1>
      <p class="brand-panel__summary">统一连接学生、配送员和管理员，清晰掌握包裹从入校到签收的每一步。</p>
    </div>

    <ul class="brand-panel__roles" aria-label="系统角色">
      <li v-for="item in roleItems" :key="item.code" class="brand-panel__role">
        <span class="brand-panel__role-code">{{ item.code.slice(0, 1) }}</span>
        <span>
          <strong>{{ item.label }}</strong>
          <small>{{ item.description }}</small>
        </span>
      </li>
    </ul>
  </section>
</template>

<style scoped>
.brand-panel {
  position: relative;
  min-height: 640px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  padding: 48px;
  color: #fff;
  background: linear-gradient(145deg, #052f53 0%, #075486 56%, #0b73a6 100%);
}

.brand-panel__glow {
  position: absolute;
  width: 420px;
  height: 420px;
  right: -190px;
  top: -180px;
  border-radius: 50%;
  background: rgb(113 209 255 / 24%);
  box-shadow: 0 0 120px rgb(113 209 255 / 18%);
}

.brand-panel__identity,
.brand-panel__content,
.brand-panel__roles { position: relative; z-index: 1; }

.brand-panel__identity { display: flex; align-items: center; gap: 18px; }
.brand-panel__name { display: block; font-size: 28px; letter-spacing: .08em; }
.brand-panel__english { display: block; margin-top: 3px; color: #a8dbf6; font-size: 11px; letter-spacing: .3em; }
.brand-panel__content { margin: auto 0 40px; }
.brand-panel__eyebrow { margin: 0 0 16px; color: #89d2f7; font-size: 13px; font-weight: 700; letter-spacing: .18em; }
.brand-panel__title { margin: 0; font-size: clamp(34px, 4vw, 52px); line-height: 1.25; letter-spacing: -.03em; }
.brand-panel__summary { max-width: 480px; margin: 22px 0 0; color: #c7e6f6; line-height: 1.8; }
.brand-panel__roles { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; margin: 0; padding: 0; list-style: none; }
.brand-panel__role { display: flex; align-items: center; gap: 10px; padding: 14px; border: 1px solid rgb(255 255 255 / 14%); border-radius: 14px; background: rgb(255 255 255 / 8%); }
.brand-panel__role-code { display: grid; flex: 0 0 30px; height: 30px; place-items: center; border-radius: 9px; color: #075486; background: #dff4ff; font-size: 12px; font-weight: 800; }
.brand-panel__role strong, .brand-panel__role small { display: block; }
.brand-panel__role strong { font-size: 13px; }
.brand-panel__role small { margin-top: 3px; color: #a9cee2; font-size: 10px; }

@media (max-width: 900px) {
  .brand-panel { min-height: 360px; padding: 32px; }
  .brand-panel__content { margin: 48px 0 30px; }
  .brand-panel__roles { display: none; }
}

@media (max-width: 560px) {
  .brand-panel { min-height: 260px; padding: 24px; }
  .brand-panel__identity :deep(.brand-mark) { width: 54px; height: 54px; }
  .brand-panel__name { font-size: 23px; }
  .brand-panel__content { margin: 34px 0 0; }
  .brand-panel__title { font-size: 29px; }
  .brand-panel__summary { display: none; }
}
</style>
