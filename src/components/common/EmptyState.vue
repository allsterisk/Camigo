<script setup lang="ts">
withDefaults(defineProps<{
  title?: string
  description?: string
  actionLabel?: string
}>(), {
  title: '暂无数据',
  description: '这里暂时没有可展示的内容',
  actionLabel: '',
})

defineEmits<{ action: [] }>()
</script>

<template>
  <el-empty :description="title" :image-size="110">
    <p class="empty-state__description">{{ description }}</p>
    <el-button v-if="actionLabel" type="primary" plain @click="$emit('action')">{{ actionLabel }}</el-button>
  </el-empty>
</template>

<style scoped>
.empty-state__description { margin: -8px 0 18px; color: #8a9bab; font-size: 13px; }
</style>
