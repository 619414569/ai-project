<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { DataBoard, Notebook } from '@element-plus/icons-vue'

const route = useRoute()
const activePath = computed(() => route.path)
</script>

<template>
  <div class="app-shell">
    <aside class="app-sidebar">
      <div class="brand-block">
        <span class="brand-mark">N</span>
        <div>
          <strong>Notebook</strong>
          <small>内存存储示例</small>
        </div>
      </div>

      <el-menu :default-active="activePath" router class="app-menu">
        <el-menu-item index="/">
          <el-icon><Notebook /></el-icon>
          <span>手账</span>
        </el-menu-item>
        <el-menu-item index="/status">
          <el-icon><DataBoard /></el-icon>
          <span>运行概览</span>
        </el-menu-item>
      </el-menu>

      <div class="sidebar-footer">
        <el-tag type="info" effect="plain" round>无需登录</el-tag>
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
