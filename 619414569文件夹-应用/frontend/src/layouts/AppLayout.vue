<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Bell, Connection, Document, Grid, HomeFilled, Key, Link, SwitchButton, User } from '@element-plus/icons-vue'
import PasswordChangeDialog from '@/components/PasswordChangeDialog.vue'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const passwordDialogVisible = ref(false)
const activePath = computed(() => (route.name === 'dashboard' ? route.fullPath : route.path))

watch(
  () => authStore.passwordChangeRequired,
  (required) => {
    if (required) passwordDialogVisible.value = true
  },
  { immediate: true },
)

function logout() {
  authStore.logout()
  void router.replace('/login')
}
</script>

<template>
  <div class="app-shell">
    <aside class="app-sidebar">
      <div class="brand-block">
        <span class="brand-mark"><el-icon><Grid /></el-icon></span>
        <div>
          <strong>AI 工作台</strong>
          <small>应用创建与协作</small>
        </div>
      </div>

      <el-menu :default-active="activePath" router class="app-menu">
        <el-menu-item index="/">
          <el-icon><HomeFilled /></el-icon>
          <span>工作台</span>
        </el-menu-item>
        <el-menu-item index="/#my-applications">
          <el-icon><Grid /></el-icon>
          <span>我的应用</span>
        </el-menu-item>
        <el-menu-item index="/#other-applications">
          <el-icon><User /></el-icon>
          <span>他人应用</span>
        </el-menu-item>
        <el-menu-item index="/#collaboration-applications">
          <el-icon><Connection /></el-icon>
          <span>合作应用</span>
        </el-menu-item>
        <el-menu-item index="/#submission-logs">
          <el-icon><Document /></el-icon>
          <span>提交日志</span>
        </el-menu-item>
        <el-menu-item index="/integrations">
          <el-icon><Link /></el-icon>
          <span>组件联调</span>
        </el-menu-item>
      </el-menu>

      <div class="sidebar-footer">
        <div class="user-summary">
          <span class="user-avatar">{{ authStore.profile?.username.slice(0, 1).toUpperCase() }}</span>
          <div>
            <strong>{{ authStore.profile?.username }}</strong>
            <small>{{ authStore.profile?.roles.join(' / ') }}</small>
          </div>
        </div>
        <el-tooltip content="退出登录" placement="top">
          <el-button :icon="SwitchButton" circle text aria-label="退出登录" @click="logout" />
        </el-tooltip>
      </div>
    </aside>

    <main class="app-main">
      <header class="app-header">
        <div>
          <span class="section-kicker">AI PROJECT</span>
          <h1>{{ route.meta.title }}</h1>
        </div>
        <div class="header-actions">
          <el-tooltip content="通知" placement="bottom">
            <el-badge :value="2" :max="9" class="notification-badge">
              <el-button :icon="Bell" circle text aria-label="通知" />
            </el-badge>
          </el-tooltip>
          <el-dropdown trigger="click">
            <button type="button" class="header-user">
              <span class="user-avatar">{{ authStore.profile?.username.slice(0, 1).toUpperCase() }}</span>
              <span><strong>{{ authStore.profile?.username }}</strong><small>{{ authStore.profile?.roles[0] }}</small></span>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item :icon="Key" @click="passwordDialogVisible = true">修改密码</el-dropdown-item>
                <el-dropdown-item :icon="SwitchButton" @click="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>
      <section class="page-content">
        <RouterView />
      </section>
    </main>
    <PasswordChangeDialog
      v-model="passwordDialogVisible"
      :required="authStore.passwordChangeRequired"
    />
  </div>
</template>
