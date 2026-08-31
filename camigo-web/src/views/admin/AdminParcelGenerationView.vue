<script setup lang="ts">
import { onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import AdminParcelGenerationForm from '@/components/admin/AdminParcelGenerationForm.vue'
import PageHeader from '@/components/common/PageHeader.vue'
import { useAdminParcelGeneration } from '@/composables/useAdminParcelGeneration'
import type { ParcelGenerationRequest } from '@/types/parcel-generation'

const { options, generated, loading, submitting, errorMessage, loadOptions, generate } = useAdminParcelGeneration()

async function handleGenerate(payload: ParcelGenerationRequest) {
  if (!(await generate(payload))) return
  ElMessage.success(`已生成 ${generated.value.length} 件包裹并启动智能派单`)
}
onMounted(loadOptions)
</script>

<template>
  <div class="parcel-generation">
    <PageHeader title="生成包裹" description="按真实业务约束创建单件或小批量包裹，并在同一事务中完成状态登记、配送单创建与智能派单。" refreshable :refreshing="loading" @refresh="loadOptions" />
    <el-alert v-if="errorMessage" class="parcel-generation__alert" :title="errorMessage" type="error" :closable="false" show-icon />
    <el-skeleton v-if="loading && !options" :rows="7" animated />
    <AdminParcelGenerationForm v-else-if="options" :options="options" :submitting="submitting" @submit="handleGenerate" />
    <el-empty v-else description="生成选项暂不可用"><el-button @click="loadOptions">重新加载</el-button></el-empty>

    <section v-if="generated.length" class="parcel-generation__results" aria-live="polite">
      <div class="parcel-generation__result-heading"><div><h2>本次生成结果</h2><p>{{ generated.length }} 件包裹均已完成业务登记</p></div><el-tag type="success" effect="plain">整批成功</el-tag></div>
      <div class="parcel-generation__result-list">
        <article v-for="item in generated" :key="item.parcelId" class="parcel-generation__result">
          <div><span>{{ item.carrierCode }}</span><strong>{{ item.trackingNo }}</strong><small>配送单 {{ item.orderNo }}</small></div>
          <div class="parcel-generation__status"><el-tag>{{ item.parcelStatus }}</el-tag><el-tag type="warning" effect="plain">{{ item.orderStatus }} · 第 {{ item.dispatchRound }} 轮</el-tag></div>
        </article>
      </div>
    </section>
  </div>
</template>

<style scoped>
.parcel-generation { max-width: 1040px; margin: 0 auto; }
.parcel-generation__alert { margin-bottom: 18px; }
.parcel-generation__results { margin-top: 24px; padding: 22px; border: 1px solid #cfe3df; border-radius: 18px; background: #f8fcfb; }
.parcel-generation__result-heading { display: flex; align-items: center; justify-content: space-between; gap: 16px; margin-bottom: 16px; }
.parcel-generation__result-heading h2 { margin: 0; color: #174f5b; font-size: 20px; }
.parcel-generation__result-heading p { margin: 6px 0 0; color: #718481; font-size: 13px; }
.parcel-generation__result-list { display: grid; gap: 10px; }
.parcel-generation__result { display: flex; align-items: center; justify-content: space-between; gap: 18px; padding: 14px 16px; border: 1px solid #dfebe9; border-radius: 12px; background: #fff; }
.parcel-generation__result > div:first-child { display: grid; grid-template-columns: auto 1fr; align-items: baseline; gap: 3px 9px; min-width: 0; }
.parcel-generation__result span { color: #16849a; font-size: 12px; font-weight: 800; }
.parcel-generation__result strong { color: #203e48; overflow-wrap: anywhere; }
.parcel-generation__result small { grid-column: 2; color: #788987; }
.parcel-generation__status { display: flex; flex-wrap: wrap; justify-content: flex-end; gap: 8px; }
@media (max-width: 680px) { .parcel-generation__results { padding: 16px; } .parcel-generation__result { align-items: flex-start; flex-direction: column; } .parcel-generation__status { justify-content: flex-start; } }
</style>
