<script setup lang="ts">
import { reactive, shallowRef } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import type { ParcelException, ParcelExceptionResolution } from '@/types/parcel-exception'

const visible = defineModel<boolean>({ required: true })
const props = withDefaults(defineProps<{ record?: ParcelException | null; submitting?: boolean; errorMessage?: string }>(), {
  record: null, submitting: false, errorMessage: '',
})
const emit = defineEmits<{ submit: [type: ParcelExceptionResolution, note: string]; closed: [] }>()
const formRef = shallowRef<FormInstance>()
const form = reactive<{ resolutionType: ParcelExceptionResolution | ''; resolutionNote: string }>({ resolutionType: '', resolutionNote: '' })
const rules: FormRules = {
  resolutionType: [{ required: true, message: '请选择处理方式', trigger: 'change' }],
  resolutionNote: [{ required: true, message: '请填写处理说明', trigger: 'blur' }, { max: 500, message: '不能超过500个字符', trigger: 'blur' }],
}
const choices: { value: ParcelExceptionResolution; title: string; detail: string }[] = [
  { value: 'RESUME_DELIVERY', title: '恢复配送', detail: '问题已解决，包裹仍由原配送员继续送往目的地。' },
  { value: 'RETURN_TO_SORTING', title: '恢复服务站分拣', detail: '包裹已经回到原取件服务站，重新进入分拣状态。' },
  { value: 'CONFIRM_RETURNED', title: '确认已经退回', detail: '包裹已经实际回到原取件服务站，结束本次配送。' },
]
async function handleSubmit() {
  if (!formRef.value || !(await formRef.value.validate().catch(() => false))) return
  emit('submit', form.resolutionType as ParcelExceptionResolution, form.resolutionNote.trim())
}
function handleClosed() { form.resolutionType = ''; form.resolutionNote = ''; formRef.value?.clearValidate(); emit('closed') }
</script>

<template>
  <el-dialog v-model="visible" title="处理包裹异常" width="min(620px, calc(100vw - 32px))" :show-close="!props.submitting" :close-on-click-modal="!props.submitting" @closed="handleClosed">
    <div v-if="props.record" class="resolution-dialog__summary"><strong>{{ props.record.trackingNo }}</strong><span>{{ props.record.description }}</span></div>
    <el-alert v-if="props.errorMessage" class="resolution-dialog__alert" :title="props.errorMessage" type="error" :closable="false" show-icon />
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <el-form-item label="处理方式" prop="resolutionType">
        <el-radio-group v-model="form.resolutionType" class="resolution-dialog__choices">
          <el-radio v-for="choice in choices" :key="choice.value" :value="choice.value" border>
            <span class="resolution-dialog__choice"><strong>{{ choice.title }}</strong><small>{{ choice.detail }}</small></span>
          </el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="处理说明" prop="resolutionNote"><el-input v-model="form.resolutionNote" type="textarea" :rows="4" maxlength="500" show-word-limit placeholder="记录核实结果、交接情况和处理依据" /></el-form-item>
    </el-form>
    <template #footer><el-button :disabled="props.submitting" @click="visible = false">取消</el-button><el-button type="primary" :loading="props.submitting" @click="handleSubmit">确认处理</el-button></template>
  </el-dialog>
</template>

<style scoped>
.resolution-dialog__summary { display: grid; gap: 6px; margin-bottom: 18px; padding: 14px 16px; border-radius: 14px; background: #f1f7f6; color: #405957; overflow-wrap: anywhere; }
.resolution-dialog__alert { margin-bottom: 16px; }
.resolution-dialog__choices { display: grid; gap: 10px; width: 100%; }
.resolution-dialog__choices :deep(.el-radio) { width: 100%; height: auto; min-height: 68px; margin: 0; padding: 12px 14px; white-space: normal; }
.resolution-dialog__choice { display: grid; gap: 4px; line-height: 1.45; }
.resolution-dialog__choice small { color: #687a78; font-size: 13px; }
</style>
