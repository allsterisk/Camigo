<script setup lang="ts">
import { computed } from 'vue'

import { getStatusDisplay } from '@/utils/parcel'

const props = defineProps<{ status: string }>()
const display = computed(() => getStatusDisplay(props.status))
</script>

<template>
  <el-tag :type="display.type" effect="light" round>{{ display.label }}</el-tag>
</template>
