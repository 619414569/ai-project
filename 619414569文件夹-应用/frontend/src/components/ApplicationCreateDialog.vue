<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { Connection, Link, Lock, Share, User } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import type { ApplicationScope, CreateApplicationInput } from '@/types/workspace'
import { normalizeApplicationUrl, validateApplicationUrl } from '@/utils/application-url'

const props = defineProps<{
  modelValue: boolean
  scope: ApplicationScope
  owner: string
  availableCollaborators: string[]
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  create: [application: CreateApplicationInput]
}>()

interface ApplicationForm {
  name: string
  description: string
  category: string
  visibility: 'shared' | 'exclusive'
  collaborators: string[]
  applicationUrl: string
}

const formRef = ref<FormInstance>()
const categories = ['智能问答', '数据分析', '内容生成', '流程自动化', '知识管理']
const collaboratorOptions = computed(() =>
  props.availableCollaborators.filter((username) => username !== props.owner),
)
const form = reactive<ApplicationForm>({
  name: '',
  description: '',
  category: '智能问答',
  visibility: 'exclusive',
  collaborators: [],
  applicationUrl: '',
})

const rules = computed<FormRules<ApplicationForm>>(() => ({
  name: [
    { required: true, message: '请输入应用名称', trigger: 'blur' },
    { min: 2, max: 30, message: '请输入 2 至 30 个字符', trigger: 'blur' },
  ],
  description: [{ required: true, message: '请输入应用简介', trigger: 'blur' }],
  category: [{ required: true, message: '请选择应用类型', trigger: 'change' }],
  applicationUrl: [
    {
      validator: (_rule, value: string, callback) => {
        const result = validateApplicationUrl(value)
        if (result === true) callback()
        else callback(new Error(result))
      },
      trigger: 'blur',
    },
  ],
  collaborators:
    props.scope === 'collaboration'
      ? [{ type: 'array', min: 1, required: true, message: '请选择至少一位合作人', trigger: 'change' }]
      : [],
}))

watch(
  () => props.modelValue,
  (open) => {
    if (!open) return
    form.name = ''
    form.description = ''
    form.category = '智能问答'
    form.visibility = props.scope === 'collaboration' ? 'shared' : 'exclusive'
    form.collaborators = []
    form.applicationUrl = ''
    formRef.value?.clearValidate()
  },
)

function close() {
  emit('update:modelValue', false)
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  emit('create', {
    name: form.name.trim(),
    description: form.description.trim(),
    category: form.category,
    scope: props.scope,
    visibility: form.visibility,
    owner: props.owner,
    collaborators: [...form.collaborators],
    applicationUrl: form.applicationUrl ? normalizeApplicationUrl(form.applicationUrl) : undefined,
  })
  close()
}
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    :title="scope === 'personal' ? '创建个人应用' : '创建合作应用'"
    width="560px"
    destroy-on-close
    class="application-dialog"
    @close="close"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <div class="dialog-form-grid">
        <el-form-item label="应用名称" prop="name" class="dialog-form-wide">
          <el-input v-model="form.name" maxlength="30" show-word-limit placeholder="例如：项目知识问答" />
        </el-form-item>
        <el-form-item label="应用类型" prop="category">
          <el-select v-model="form.category" class="field-full-width">
            <el-option v-for="category in categories" :key="category" :label="category" :value="category" />
          </el-select>
        </el-form-item>
        <el-form-item label="访问方式" prop="visibility">
          <el-radio-group v-model="form.visibility" class="visibility-selector">
            <el-radio-button value="exclusive">
              <el-icon><Lock /></el-icon>
              独享
            </el-radio-button>
            <el-radio-button value="shared">
              <el-icon><Share /></el-icon>
              共享
            </el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item
          v-if="scope === 'collaboration'"
          label="合作人"
          prop="collaborators"
          class="dialog-form-wide"
        >
          <el-select
            v-model="form.collaborators"
            multiple
            filterable
            collapse-tags
            collapse-tags-tooltip
            placeholder="选择合作成员"
            class="field-full-width"
          >
            <el-option v-for="user in collaboratorOptions" :key="user" :label="user" :value="user">
              <span class="collaborator-option"><el-icon><User /></el-icon>{{ user }}</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="应用地址（选填）" prop="applicationUrl" class="dialog-form-wide">
          <el-input
            v-model="form.applicationUrl"
            :prefix-icon="Link"
            placeholder="例如：https://app.example.com 或 /internal-app"
            clearable
          />
        </el-form-item>
        <el-form-item label="应用简介" prop="description" class="dialog-form-wide">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            maxlength="120"
            show-word-limit
            resize="none"
            placeholder="说明应用解决的问题和主要使用对象"
          />
        </el-form-item>
      </div>
    </el-form>

    <template #footer>
      <el-button @click="close">取消</el-button>
      <el-button type="primary" :icon="scope === 'personal' ? User : Connection" @click="submit">
        创建应用
      </el-button>
    </template>
  </el-dialog>
</template>
