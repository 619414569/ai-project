<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { DataBoard, Link, SwitchButton } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const activePath = computed(() => route.path)

function logout() {
  authStore.logout()
  void router.replace('/login')
}
</script>

<template>
  <div class="app-shell">
    <aside class="app-sidebar">
      <div class="brand-block">
        <span class="brand-mark">AI</span>
        <div>
          <strong>AI Project</strong>
          <small>通用研发底座</small>
        </div>
      </div>

      <el-menu :default-active="activePath" router class="app-menu">
        <el-menu-item index="/">
          <el-icon><DataBoard /></el-icon>
          <span>运行概览</span>
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
          <span class="section-kicker">PLATFORM CONSOLE</span>
          <h1>{{ route.meta.title }}</h1>
        </div>
        <el-tag type="success" effect="plain" round>开发环境</el-tag>
      </header>
      <section class="page-content">
        <RouterView />
      </section>
    </main>
  </div>
</template>
