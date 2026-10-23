<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { Key, Lock } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const props = defineProps<{
  modelValue: boolean
  required?: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

interface PasswordForm {
  currentPassword: string
  newPassword: string
  confirmPassword: string
}

const authStore = useAuthStore()
const formRef = ref<FormInstance>()
const submitting = ref(false)
const form = reactive<PasswordForm>({
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const rules = computed<FormRules<PasswordForm>>(() => ({
  currentPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, max: 64, message: '密码长度为 8 至 64 个字符', trigger: 'blur' },
    {
      pattern: /^(?=.*[A-Za-z])(?=.*\d).+$/,
      message: '密码必须同时包含字母和数字',
      trigger: 'blur',
    },
    {
      validator: (_rule, value: string, callback) => {
        if (value && value === form.currentPassword) callback(new Error('新密码不能与当前密码相同'))
        else callback()
      },
      trigger: 'blur',
    },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule, value: string, callback) => {
        if (value !== form.newPassword) callback(new Error('两次输入的密码不一致'))
        else callback()
      },
      trigger: 'blur',
    },
  ],
}))

watch(
  () => props.modelValue,
  (open) => {
    if (!open) return
    form.currentPassword = ''
    form.newPassword = ''
    form.confirmPassword = ''
    formRef.value?.clearValidate()
  },
)

function close() {
  emit('update:modelValue', false)
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await authStore.changePassword({
      currentPassword: form.currentPassword,
      newPassword: form.newPassword,
    })
    ElMessage.success('密码已修改')
    close()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '密码修改失败')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    :title="required ? '首次登录，请修改密码' : '修改密码'"
    width="460px"
    destroy-on-close
    class="password-dialog"
    @close="close"
  >
    <el-alert
      v-if="required"
      title="当前账号需要修改初始密码，为保障账号安全，请设置新密码。"
      type="warning"
      :closable="false"
      show-icon
      class="password-reminder"
    />
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <el-form-item label="当前密码" prop="currentPassword">
        <el-input
          v-model="form.currentPassword"
          type="password"
          show-password
          :prefix-icon="Lock"
          autocomplete="current-password"
        />
      </el-form-item>
      <el-form-item label="新密码" prop="newPassword">
        <el-input
          v-model="form.newPassword"
          type="password"
          show-password
          :prefix-icon="Key"
          autocomplete="new-password"
          placeholder="至少 8 位，包含字母和数字"
        />
      </el-form-item>
      <el-form-item label="确认新密码" prop="confirmPassword">
        <el-input
          v-model="form.confirmPassword"
          type="password"
          show-password
          :prefix-icon="Key"
          autocomplete="new-password"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button :disabled="submitting" @click="close">{{ required ? '稍后修改' : '取消' }}</el-button>
      <el-button type="primary" :icon="Key" :loading="submitting" @click="submit">确认修改</el-button>
    </template>
  </el-dialog>
</template>
