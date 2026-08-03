<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { acceptDeliveryOrder, getDeliveryTasks, updateDeliveryOrderStatus } from '../api/deliveryOrders'

const loading = ref(false)
const operatingId = ref(null)
const rows = ref([])
const total = ref(0)
const query = reactive({ page: 1, pageSize: 20, status: '' })
const statusLabels = { WAITING: '待接单', PROCESSING: '配送中', DELIVERED: '已送达' }

async function load() {
  loading.value = true
  try {
    const result = await getDeliveryTasks({
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

async function acceptTask(task) {
  operatingId.value = task.id
  try {
    await acceptDeliveryOrder(task.id)
    ElMessage.success('接单成功')
    await load()
  } catch {
    // 统一错误提示由 Axios 拦截器处理。
  } finally {
    operatingId.value = null
  }
}

async function completeTask(task) {
  try {
    await ElMessageBox.confirm('确认该任务已经送达目标位置？', '确认送达', { type: 'warning' })
  } catch {
    return
  }
  operatingId.value = task.id
  try {
    await updateDeliveryOrderStatus(task.id, 'DELIVERED')
    ElMessage.success('配送任务已送达')
    await load()
  } catch {
    // 统一错误提示由 Axios 拦截器处理。
  } finally {
    operatingId.value = null
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
      <div><p class="eyebrow">DELIVERY</p><h1>配送任务</h1></div>
      <el-segmented v-model="query.status" :options="[
        { label: '全部', value: '' },
        { label: '待接单', value: 'WAITING' },
        { label: '配送中', value: 'PROCESSING' },
        { label: '已送达', value: 'DELIVERED' },
      ]" @change="query.page = 1; load()" />
    </div>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="rows" empty-text="暂无配送任务">
        <el-table-column prop="orderNo" label="任务单号" min-width="200" />
        <el-table-column prop="parcelId" label="快递编号" width="110" />
        <el-table-column prop="destinationLocationId" label="目的地" width="100" />
        <el-table-column prop="priority" label="优先级" width="100" />
        <el-table-column prop="predictedMinutes" label="预计分钟" width="100" />
        <el-table-column label="状态" width="110">
          <template #default="scope"><el-tag effect="plain">{{ statusLabels[scope.row.status] || scope.row.status }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="scope">
            <el-button v-if="scope.row.status === 'WAITING'" link type="primary" :loading="operatingId === scope.row.id" @click="acceptTask(scope.row)">接单</el-button>
            <el-button v-else-if="scope.row.status === 'PROCESSING'" link type="success" :loading="operatingId === scope.row.id" @click="completeTask(scope.row)">确认送达</el-button>
            <span v-else class="muted">已完成</span>
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
