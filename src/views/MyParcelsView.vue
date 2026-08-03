<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getMyParcels, updateParcelStatus } from '../api/parcels'

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const query = reactive({ page: 1, pageSize: 20, status: '' })
const statusOptions = ['ARRIVED', 'DELIVERING', 'STORED', 'PICKED', 'COMPLETED']
const statusLabels = {
  ARRIVED: '已到校', DELIVERING: '配送中', STORED: '待取件', PICKED: '已取件', COMPLETED: '已完成',
}

async function load() {
  loading.value = true
  try {
    const result = await getMyParcels({
      page: query.page,
      pageSize: query.pageSize,
      status: query.status || undefined,
    })
    rows.value = result.records
    total.value = result.total
  } catch {
    rows.value = []
  } finally {
    loading.value = false
  }
}

async function confirmCompleted(parcel) {
  try {
    await updateParcelStatus(parcel.id, { status: 'COMPLETED', remark: '用户在网页端确认完成' })
    ElMessage.success('已确认快递完成')
    await load()
  } catch {
    // 统一错误提示由 Axios 拦截器处理。
  }
}

function changePage(page) {
  query.page = page
  load()
}

onMounted(load)
</script>

<template>
  <section>
    <div class="page-heading">
      <div><p class="eyebrow">PARCELS</p><h1>我的快递</h1></div>
      <el-select v-model="query.status" clearable placeholder="全部状态" style="width: 160px" @change="query.page = 1; load()">
        <el-option v-for="status in statusOptions" :key="status" :label="statusLabels[status]" :value="status" />
      </el-select>
    </div>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="rows" empty-text="暂无快递">
        <el-table-column prop="trackingNo" label="快递单号" min-width="180" />
        <el-table-column prop="carrierCode" label="承运商" width="100" />
        <el-table-column prop="senderName" label="寄件人" min-width="120" />
        <el-table-column label="状态" width="110">
          <template #default="scope"><el-tag effect="plain">{{ statusLabels[scope.row.currentStatus] || scope.row.currentStatus }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="arriveTime" label="到校时间" min-width="180" />
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="scope">
            <el-button v-if="scope.row.currentStatus === 'PICKED'" link type="primary" @click="confirmCompleted(scope.row)">确认完成</el-button>
            <span v-else class="muted">—</span>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-if="total > query.pageSize"
        class="table-pagination"
        background
        layout="prev, pager, next"
        :total="total"
        :page-size="query.pageSize"
        :current-page="query.page"
        @current-change="changePage"
      />
    </el-card>
  </section>
</template>
