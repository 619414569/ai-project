<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { demoApi } from '@/api/modules/demo'
import type { PingResponse } from '@/types/api'

const loading = ref(false)
const ping = ref<PingResponse | null>(null)

const capabilities = [
  { name: 'Spring Boot Actuator', detail: '健康检查端点', state: '已启用' },
  { name: 'OpenAPI', detail: 'Swagger UI 在线调试', state: '已启用' },
  { name: 'Nginx', detail: 'SPA 托管 + API 代理', state: '已配置' },
]

async function refresh() {
  loading.value = true
  try {
    ping.value = await demoApi.ping()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '后端连接失败')
  } finally {
    loading.value = false
  }
}

onMounted(refresh)
</script>

<template>
  <div class="page-stack">
    <section class="status-band">
      <div>
        <span class="status-dot" :class="{ online: ping?.status === 'UP' }" />
        <strong>{{ ping?.status === 'UP' ? '后端服务在线' : '正在检测后端服务' }}</strong>
        <span v-if="ping" class="muted">{{ ping.application }}</span>
      </div>
      <el-button :icon="Refresh" :loading="loading" @click="refresh">重新检测</el-button>
    </section>

    <section>
      <div class="section-heading">
        <div>
          <p class="section-kicker">FOUNDATION</p>
          <h2>基础能力</h2>
        </div>
        <span class="muted">{{ capabilities.length }} 个标准组件</span>
      </div>

      <div class="capability-grid">
        <article v-for="item in capabilities" :key="item.name" class="capability-item">
          <div>
            <h3>{{ item.name }}</h3>
            <p>{{ item.detail }}</p>
          </div>
          <el-tag type="success" effect="plain">{{ item.state }}</el-tag>
        </article>
      </div>
    </section>

    <section class="delivery-strip">
      <div>
        <span class="metric">2</span>
        <span>独立交付单元</span>
      </div>
      <div>
        <span class="metric">1</span>
        <span>命令整套启动</span>
      </div>
      <div>
        <span class="metric">0</span>
        <span>外部中间件依赖</span>
      </div>
    </section>
  </div>
</template>
