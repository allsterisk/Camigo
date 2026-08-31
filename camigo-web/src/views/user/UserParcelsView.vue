<script setup lang="ts">
import { onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'

import EmptyState from '@/components/common/EmptyState.vue'
import PageHeader from '@/components/common/PageHeader.vue'
import ParcelFilters from '@/components/user/ParcelFilters.vue'
import ParcelTable from '@/components/user/ParcelTable.vue'
import { useUserParcels } from '@/composables/useUserParcels'
import type { ParcelStatus } from '@/types/parcel'

const router = useRouter()
const filters = reactive<{ trackingNo: string; status: ParcelStatus | '' }>({ trackingNo: '', status: '' })
const { parcels, total, page, pageSize, loading, errorMessage, load } = useUserParcels()

function query(nextPage = 1) {
  return load({
    page: nextPage,
    pageSize: 10,
    trackingNo: filters.trackingNo || undefined,
    status: filters.status || undefined,
  })
}

function reset() {
  filters.trackingNo = ''
  filters.status = ''
  query()
}

onMounted(() => query())
</script>

<template>
  <div>
    <PageHeader title="我的快递" description="按快递单号或状态查询属于你的校园包裹。" refreshable :refreshing="loading" @refresh="query(page)" />
    <ParcelFilters v-model:tracking-no="filters.trackingNo" v-model:status="filters.status" :loading="loading" @search="query()" @reset="reset" />
    <el-alert v-if="errorMessage" class="user-parcels__error" :title="errorMessage" type="error" :closable="false" show-icon />
    <section class="user-parcels__table-card">
      <EmptyState v-if="!loading && parcels.length === 0" title="没有符合条件的快递" description="可以调整筛选条件，或等待管理员录入新快递。" action-label="清空筛选" @action="reset" />
      <template v-else>
        <ParcelTable :parcels="parcels" :loading="loading" @view="(id) => router.push(`/user/parcels/${id}`)" />
        <el-pagination
          v-if="total > pageSize"
          class="user-parcels__pagination"
          background
          layout="prev, pager, next, total"
          :current-page="page"
          :page-size="pageSize"
          :total="total"
          @current-change="query"
        />
      </template>
    </section>
  </div>
</template>

<style scoped>
.user-parcels__error { margin-bottom: 18px; }
.user-parcels__table-card { min-height: 260px; padding: 20px; border: 1px solid #e0eaf2; border-radius: 18px; background: #fff; box-shadow: 0 10px 28px rgb(15 59 92 / 5%); }
.user-parcels__pagination { justify-content: flex-end; margin-top: 22px; }
</style>
