<script setup lang="ts">
import { reactive, shallowRef } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import type { ParcelGenerationOptions, ParcelGenerationRequest, ParcelSizeClass } from '@/types/parcel-generation'

const props = defineProps<{ options: ParcelGenerationOptions; submitting?: boolean }>()
const emit = defineEmits<{ submit: [payload: ParcelGenerationRequest] }>()
const formRef = shallowRef<FormInstance>()
const form = reactive({
  recipientUserId: undefined as number | undefined,
  pickupLocationId: undefined as number | undefined,
  destinationLocationId: undefined as number | undefined,
  sizeClass: 'M' as ParcelSizeClass,
  weightKg: 1,
  arriveTime: '',
  count: 1,
})
const rules: FormRules = {
  recipientUserId: [{ required: true, message: '请选择收件学生', trigger: 'change' }],
  pickupLocationId: [{ required: true, message: '请选择取件服务站', trigger: 'change' }],
  destinationLocationId: [{ required: true, message: '请选择配送目的地', trigger: 'change' }],
  sizeClass: [{ required: true, message: '请选择包裹尺寸', trigger: 'change' }],
  weightKg: [{ required: true, message: '请输入包裹重量', trigger: 'blur' }],
  count: [{ required: true, message: '请输入生成数量', trigger: 'blur' }],
}

async function handleSubmit() {
  if (props.submitting || !formRef.value || !(await formRef.value.validate().catch(() => false))) return
  if (!form.recipientUserId || !form.pickupLocationId || !form.destinationLocationId) return
  emit('submit', {
    recipientUserId: form.recipientUserId,
    pickupLocationId: form.pickupLocationId,
    destinationLocationId: form.destinationLocationId,
    sizeClass: form.sizeClass,
    weightKg: form.weightKg,
    arriveTime: form.arriveTime || undefined,
    count: form.count,
  })
}
</script>

<template>
  <el-form ref="formRef" class="generation-form" :model="form" :rules="rules" label-position="top" @submit.prevent="handleSubmit">
    <div class="generation-form__grid">
      <el-form-item label="收件学生" prop="recipientUserId">
        <el-select v-model="form.recipientUserId" filterable placeholder="按姓名、学号选择" no-data-text="暂无有效学生">
          <el-option v-for="student in options.students" :key="student.id" :value="student.id" :label="`${student.realName} · ${student.studentNo || student.phone}`" />
        </el-select>
      </el-form-item>
      <el-form-item label="取件服务站" prop="pickupLocationId">
        <el-select v-model="form.pickupLocationId" placeholder="选择包裹所在服务站" no-data-text="暂无可用服务站">
          <el-option v-for="location in options.pickupLocations" :key="location.id" :value="location.id" :label="location.locationName" />
        </el-select>
      </el-form-item>
      <el-form-item label="配送目的地" prop="destinationLocationId">
        <el-select v-model="form.destinationLocationId" filterable placeholder="选择宿舍或配送点" no-data-text="暂无可配送地点">
          <el-option v-for="location in options.destinationLocations" :key="location.id" :value="location.id" :label="location.locationName" />
        </el-select>
      </el-form-item>
      <el-form-item label="包裹尺寸" prop="sizeClass">
        <el-segmented v-model="form.sizeClass" :options="options.sizeClasses" />
      </el-form-item>
      <el-form-item label="重量（千克）" prop="weightKg">
        <el-input-number v-model="form.weightKg" :min="0.001" :max="99999.999" :precision="3" :step="0.001" controls-position="right" />
      </el-form-item>
      <el-form-item label="到站时间（可选）" prop="arriveTime">
        <el-date-picker v-model="form.arriveTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" placeholder="默认使用当前时间" :disabled-date="(date: Date) => date.getTime() > Date.now()" />
      </el-form-item>
      <el-form-item label="生成数量" prop="count">
        <el-input-number v-model="form.count" :min="1" :max="options.maxBatchSize" controls-position="right" />
        <span class="generation-form__hint">单次最多 {{ options.maxBatchSize }} 件，整批成功或整批回滚。</span>
      </el-form-item>
    </div>
    <div class="generation-form__notice">
      <strong>生成后立即进入智能派单</strong>
      <span>系统会自动补全承运商、运单号和寄件人信息，并写入注册与待分配日志。</span>
    </div>
    <el-button native-type="submit" type="primary" size="large" :loading="submitting" :disabled="!options.students.length || !options.pickupLocations.length || !options.destinationLocations.length">
      {{ form.count > 1 ? `生成 ${form.count} 件并派单` : '生成并派单' }}
    </el-button>
  </el-form>
</template>

<style scoped>
.generation-form { padding: 24px; border: 1px solid #dce8e7; border-radius: 18px; background: #fff; box-shadow: 0 12px 32px rgb(12 71 68 / 6%); }
.generation-form__grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); column-gap: 24px; }
.generation-form :deep(.el-select), .generation-form :deep(.el-date-editor), .generation-form :deep(.el-input-number) { width: 100%; }
.generation-form__hint { display: block; margin-top: 7px; color: #748684; font-size: 12px; line-height: 1.5; }
.generation-form__notice { display: grid; gap: 4px; margin: 2px 0 22px; padding: 14px 16px; border: 1px solid #d5e9e7; border-radius: 10px; background: #f1f8f8; color: #536d6b; line-height: 1.55; }
.generation-form__notice strong { color: #164c5c; }
@media (max-width: 760px) { .generation-form { padding: 18px; } .generation-form__grid { grid-template-columns: 1fr; } .generation-form :deep(.el-segmented) { width: 100%; } }
</style>
