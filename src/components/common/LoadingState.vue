<template>
  <div class="loading-state" aria-label="正在加载">
    <el-skeleton :rows="6" animated />
  </div>
</template>

<style scoped>
.loading-state { padding: 28px; border: 1px solid #e5edf4; border-radius: 18px; background: #fff; }
</style>
