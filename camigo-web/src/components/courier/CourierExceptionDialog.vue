<script setup lang="ts">
import { reactive, shallowRef } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import type { ParcelExceptionType } from '@/types/parcel-exception'

const visible = defineModel<boolean>({ required: true })
const props = withDefaults(defineProps<{ orderId?: number | null; submitting?: boolean; errorMessage?: string }>(), {
  orderId: null, submitting: false, errorMessage: '',
})
const emit = defineEmits<{ submit: [type: ParcelExceptionType, description: string]; closed: [] }>()
const formRef = shallowRef<FormInstance>()
const form = reactive<{ exceptionType: ParcelExceptionType | ''; description: string }>({ exceptionType: '', description: '' })
const rules: FormRules = {
  exceptionType: [{ required: true, message: '请选择异常类型', trigger: 'change' }],
  description: [{ required: true, message: '请说明现场情况', trigger: 'blur' }, { max: 500, message: '不能超过500个字符', trigger: 'blur' }],
}
const options: { value: ParcelExceptionType; label: string }[] = [
  { value: 'PACKAGE_DAMAGED', label: '包裹破损' },
  { value: 'RECIPIENT_INFO_ERROR', label: '收件信息错误' },
  { value: 'LOCKER_FAILURE', label: '柜机故障' },
  { value: 'USER_UNREACHABLE', label: '无法联系用户' },
  { value: 'OTHER', label: '其他异常' },
]
async function handleSubmit() {
  if (!formRef.value || !(await formRef.value.validate().catch(() => false))) return
  emit('submit', form.exceptionType as ParcelExceptionType, form.description.trim())
}
function handleClosed() {
  form.exceptionType = ''; form.description = ''; formRef.value?.clearValidate(); emit('closed')
}
</script>

<template>
  <el-dialog v-model="visible" title="上报配送异常" width="min(520px, calc(100vw - 32px))"
    :close-on-click-modal="!props.submitting" :close-on-press-escape="!props.submitting"
    :show-close="!props.submitting" @closed="handleClosed">
    <p class="exception-dialog__intro">任务 #{{ props.orderId }} 上报后将暂停配送并从当前路线移除，必须由管理员处理。</p>
    <el-alert v-if="props.errorMessage" class="exception-dialog__alert" :title="props.errorMessage"
      type="error" :closable="false" show-icon />
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <el-form-item label="异常类型" prop="exceptionType">
        <el-select v-model="form.exceptionType" placeholder="选择现场异常" class="exception-dialog__field">
          <el-option v-for="option in options" :key="option.value" :label="option.label" :value="option.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="现场说明" prop="description">
        <el-input v-model="form.description" type="textarea" :rows="4" maxlength="500" show-word-limit
          resize="vertical" placeholder="说明包裹、联系人或柜机的实际情况，便于管理员判断" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="props.submitting" @click="visible = false">暂不上报</el-button>
      <el-button type="danger" :loading="props.submitting" @click="handleSubmit">提交并暂停任务</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.exception-dialog__intro { margin: 0 0 18px; color: #526765; line-height: 1.7; overflow-wrap: anywhere; }
.exception-dialog__alert { margin-bottom: 16px; }
.exception-dialog__field { width: 100%; }
</style>
