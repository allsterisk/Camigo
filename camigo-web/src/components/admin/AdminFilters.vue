<script setup lang="ts">
interface Option { label: string; value: string }
withDefaults(defineProps<{ placeholder: string; statusLabel?: string; options?: readonly Option[]; loading?: boolean }>(), {
  statusLabel: '状态', options: () => [], loading: false,
})
const keyword = defineModel<string>('keyword', { default: '' })
const status = defineModel<string>('status', { default: '' })
defineEmits<{ search: []; reset: [] }>()
</script>
<template>
  <section class="admin-filters">
    <el-input v-model="keyword" clearable :placeholder="placeholder" @keyup.enter="$emit('search')" />
    <el-select v-if="options.length" v-model="status" clearable :placeholder="statusLabel"><el-option v-for="option in options" :key="option.value" :label="option.label" :value="option.value" /></el-select>
    <el-button type="primary" :loading="loading" @click="$emit('search')">查询</el-button>
    <el-button @click="$emit('reset')">重置</el-button>
  </section>
</template>
<style scoped>
.admin-filters { display: grid; grid-template-columns: minmax(220px, 1fr) 180px auto auto; gap: 12px; margin-bottom: 18px; padding: 18px; border: 1px solid #e8e0ee; border-radius: 16px; background: #fff; }
@media (max-width: 720px) { .admin-filters { grid-template-columns: 1fr 1fr; } }
</style>
