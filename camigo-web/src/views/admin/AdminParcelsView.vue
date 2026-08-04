<script setup lang="ts">
import { watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import AdminFilters from '@/components/admin/AdminFilters.vue'; import AdminParcelTable from '@/components/admin/AdminParcelTable.vue'; import PageHeader from '@/components/common/PageHeader.vue'
import { useAdminListQuery } from '@/composables/useAdminListQuery'; import { useAdminParcels } from '@/composables/useAdminParcels'; import type { ParcelStatus } from '@/types/parcel'
const options = [{label:'已到校',value:'ARRIVED'},{label:'配送中',value:'DELIVERING'},{label:'待取件',value:'STORED'},{label:'已取件',value:'PICKED'},{label:'已完成',value:'COMPLETED'}]
const { route, keyword, status, page, search, reset, setPage } = useAdminListQuery('status'); const { records,total,loading,actionId,errorMessage,load,changeStatus }=useAdminParcels()
function refresh(){return load({page:page.value,pageSize:10,trackingNo:String(route.query.keyword||''),status:(route.query.status||'') as ParcelStatus|''})}
async function handleStatus(id:number,value:string){try{await ElMessageBox.confirm('确认更新该快递状态？','状态更新',{type:'warning'})}catch{return};if(await changeStatus(id,value)){ElMessage.success('快递状态已更新');await refresh()}}
watch(()=>route.query,refresh,{immediate:true})
</script>
<template><div><PageHeader eyebrow="PARCEL MANAGEMENT" title="快递管理" description="查看全平台快递，并执行符合生命周期约束的状态操作。" refreshable :refreshing="loading" @refresh="refresh"/><AdminFilters v-model:keyword="keyword" v-model:status="status" placeholder="输入快递单号" :options="options" :loading="loading" @search="search" @reset="reset"/><el-alert v-if="errorMessage" :title="errorMessage" type="error" :closable="false" show-icon/><section class="admin-list-card"><AdminParcelTable :parcels="records" :loading="loading" :action-id="actionId" @status-change="handleStatus"/><el-pagination v-if="total>10" background layout="prev, pager, next, total" :current-page="page" :page-size="10" :total="total" @current-change="setPage"/></section></div></template>
