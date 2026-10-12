<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { CircleCheck, Promotion, Refresh, Upload } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { demoApi } from '@/api/modules/demo'
import type { WorkflowTask } from '@/types/api'

const activeTab = ref('redis')
const busy = ref(false)
const cacheForm = reactive({ key: 'hello', value: 'AI Project', ttlSeconds: 300 })
const cacheResult = ref('')
const messageBody = ref('Hello RocketMQ')
const messageResult = ref('')
const processForm = reactive({ title: '基础流程验证', businessKey: `DEMO-${Date.now()}`, approver: 'admin' })
const processResult = ref('')
const tasks = ref<WorkflowTask[]>([])

async function run(action: () => Promise<void>) {
  busy.value = true
  try {
    await action()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '操作失败')
  } finally {
    busy.value = false
  }
}

function saveCache() {
  return run(async () => {
    const result = await demoApi.putCache(cacheForm.key, cacheForm.value, cacheForm.ttlSeconds)
    cacheResult.value = `已写入 ${result.key} = ${result.value}`
    ElMessage.success('Redis 写入成功')
  })
}

function readCache() {
  return run(async () => {
    const result = await demoApi.getCache(cacheForm.key)
    cacheResult.value = `${result.key} = ${result.value}`
  })
}

function sendMessage() {
  return run(async () => {
    const result = await demoApi.publishMessage(messageBody.value)
    messageResult.value = `消息 ${result.messageId} 已发送至 ${result.destination}`
    ElMessage.success('RocketMQ 消息已发送')
  })
}

function startProcess() {
  return run(async () => {
    const result = await demoApi.startApproval(processForm)
    processResult.value = `流程 ${result.processInstanceId} 已启动`
    await loadTasks()
    ElMessage.success('Flowable 流程已启动')
  })
}

async function loadTasks() {
  tasks.value = await demoApi.listTasks(processForm.approver)
}

function refreshTasks() {
  return run(loadTasks)
}

function completeTask(taskId: string) {
  return run(async () => {
    await demoApi.completeTask(taskId)
    await loadTasks()
    ElMessage.success('任务已完成')
  })
}

onMounted(() => {
  void refreshTasks()
})
</script>

<template>
  <section class="integration-panel">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="Redis" name="redis">
        <div class="tool-layout">
          <div class="tool-form">
            <h2>缓存读写</h2>
            <p class="muted">使用统一前缀和有效期验证 Redis 连接。</p>
            <el-form label-position="top">
              <el-form-item label="键">
                <el-input v-model="cacheForm.key" />
              </el-form-item>
              <el-form-item label="值">
                <el-input v-model="cacheForm.value" />
              </el-form-item>
              <el-form-item label="有效期（秒）">
                <el-input-number v-model="cacheForm.ttlSeconds" :min="1" :max="86400" />
              </el-form-item>
              <div class="button-row">
                <el-button type="primary" :icon="Upload" :loading="busy" @click="saveCache">写入</el-button>
                <el-button :icon="Refresh" :loading="busy" @click="readCache">读取</el-button>
              </div>
            </el-form>
          </div>
          <pre class="result-console">{{ cacheResult || '等待操作...' }}</pre>
        </div>
      </el-tab-pane>

      <el-tab-pane label="RocketMQ" name="rocketmq">
        <div class="tool-layout">
          <div class="tool-form">
            <h2>消息发布</h2>
            <p class="muted">消息会由示例消费者接收并写入后端日志。</p>
            <el-form label-position="top">
              <el-form-item label="消息内容">
                <el-input v-model="messageBody" type="textarea" :rows="5" maxlength="4096" show-word-limit />
              </el-form-item>
              <el-button type="primary" :icon="Promotion" :loading="busy" @click="sendMessage">发送消息</el-button>
            </el-form>
          </div>
          <pre class="result-console">{{ messageResult || '等待操作...' }}</pre>
        </div>
      </el-tab-pane>

      <el-tab-pane label="Flowable" name="flowable">
        <div class="workflow-layout">
          <div class="tool-form">
            <h2>发起审批</h2>
            <p class="muted">启动内置 BPMN 流程，并为审批人创建待办。</p>
            <el-form label-position="top">
              <el-form-item label="标题">
                <el-input v-model="processForm.title" />
              </el-form-item>
              <el-form-item label="业务编号">
                <el-input v-model="processForm.businessKey" />
              </el-form-item>
              <el-form-item label="审批人">
                <el-input v-model="processForm.approver" />
              </el-form-item>
              <el-button type="primary" :icon="Promotion" :loading="busy" @click="startProcess">发起流程</el-button>
              <p v-if="processResult" class="inline-result">{{ processResult }}</p>
            </el-form>
          </div>

          <div class="task-list">
            <div class="task-list-heading">
              <div>
                <h2>当前待办</h2>
                <p class="muted">审批人：{{ processForm.approver }}</p>
              </div>
              <el-button :icon="Refresh" circle text aria-label="刷新待办" @click="refreshTasks" />
            </div>
            <el-empty v-if="tasks.length === 0" description="暂无待办" :image-size="80" />
            <div v-for="task in tasks" :key="task.id" class="task-item">
              <div>
                <strong>{{ task.name }}</strong>
                <small>{{ task.processInstanceId }}</small>
              </div>
              <el-button type="success" :icon="CircleCheck" plain @click="completeTask(task.id)">完成</el-button>
            </div>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>
  </section>
</template>
