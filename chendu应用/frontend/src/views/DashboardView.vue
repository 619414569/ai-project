<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { demoApi } from '@/api/modules/demo'
import type { PingResponse } from '@/types/api'

const loading = ref(false)
const ping = ref<PingResponse | null>(null)

const capabilities = [
  { name: 'Spring Security', detail: 'JWT / RBAC', state: '就绪' },
  { name: 'PolarDB-PG', detail: 'PostgreSQL 协议 + Flyway', state: '已配置' },
  { name: 'Redis', detail: 'StringRedisTemplate', state: '已配置' },
  { name: 'RocketMQ', detail: '生产者 + 消费者示例', state: '已配置' },
  { name: 'Flowable', detail: '审批流程示例', state: '已配置' },
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
        <span class="metric">3</span>
        <span>独立交付单元</span>
      </div>
      <div>
        <span class="metric">1</span>
        <span>命令整套启动</span>
      </div>
      <div>
        <span class="metric">100%</span>
        <span>环境变量配置</span>
      </div>
    </section>
  </div>
</template>
