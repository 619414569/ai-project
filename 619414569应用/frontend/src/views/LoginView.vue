<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Connection, Document, Grid, Lock, User } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { DEFAULT_PASSWORD, useAuthStore } from '@/stores/auth'

interface LoginForm {
  username: string
  password: string
}

const formRef = ref<FormInstance>()
const submitting = ref(false)
const form = reactive<LoginForm>({ username: 'admin', password: DEFAULT_PASSWORD })
const rules: FormRules<LoginForm> = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await authStore.login(form)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    await router.replace(redirect)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '登录失败')
  } finally {
    submitting.value = false
  }
}

</script>

<template>
  <main class="login-page">
    <section class="login-brand">
      <span class="brand-mark brand-mark-large">AI</span>
      <p class="section-kicker">AI APPLICATION WORKBENCH</p>
      <h1>AI 应用工作台</h1>
      <p>面向团队成员的应用创建、协作和交付空间。</p>
      <div class="login-feature-list">
        <div><el-icon><Grid /></el-icon><span><strong>个人应用</strong><small>独立创建与管理</small></span></div>
        <div><el-icon><Connection /></el-icon><span><strong>团队协作</strong><small>邀请成员共同建设</small></span></div>
        <div><el-icon><Document /></el-icon><span><strong>提交追踪</strong><small>集中查看个人日志</small></span></div>
      </div>
    </section>

    <section class="login-panel">
      <div class="login-form-wrap">
        <p class="section-kicker">WELCOME BACK</p>
        <h2>登录工作台</h2>
        <p class="muted">使用你的团队账号继续。</p>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @keyup.enter="submit">
          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" :prefix-icon="User" autocomplete="username" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              :prefix-icon="Lock"
              type="password"
              show-password
              autocomplete="current-password"
            />
          </el-form-item>
          <el-button type="primary" :loading="submitting" class="login-submit" @click="submit">
            登录
          </el-button>
          <p class="login-default-password">默认密码：<code>admin123</code>，首次登录后请修改</p>
        </el-form>
      </div>
    </section>
  </main>
</template>
