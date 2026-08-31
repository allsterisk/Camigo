<script setup lang="ts">
defineProps<{
  title: string
  description?: string
  refreshable?: boolean
  refreshing?: boolean
}>()

defineEmits<{ refresh: [] }>()
</script>

<template>
  <header class="page-header">
    <div>
      <h1 class="page-header__title">{{ title }}</h1>
      <p v-if="description" class="page-header__description">{{ description }}</p>
    </div>
    <el-button v-if="refreshable" :loading="refreshing" plain @click="$emit('refresh')">
      刷新数据
    </el-button>
  </header>
</template>

<style scoped>
.page-header { display: flex; align-items: flex-end; justify-content: space-between; gap: 24px; margin-bottom: 28px; }
.page-header__title { margin: 0; color: #123c60; font-size: clamp(28px, 4vw, 38px); letter-spacing: -.03em; }
.page-header__description { margin: 10px 0 0; color: #72879a; line-height: 1.7; }

@media (max-width: 640px) {
  .page-header { align-items: flex-start; flex-direction: column; }
}
</style>
