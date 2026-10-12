<script setup lang="ts">
import { computed, ref } from 'vue'
import { Document, Search } from '@element-plus/icons-vue'
import { useApplicationStore } from '@/stores/applications'

const keyword = ref('')
const selectedUser = ref('all')
const applicationStore = useApplicationStore()
const logs = computed(() => applicationStore.submissionLogs)

const users = computed(() => [...new Set(logs.value.map((log) => log.username))])
const filteredLogs = computed(() =>
  logs.value.filter((log) => {
    const matchesUser = selectedUser.value === 'all' || log.username === selectedUser.value
    const matchesKeyword = `${log.summary}${log.area}${log.commit}`
      .toLowerCase()
      .includes(keyword.value.trim().toLowerCase())
    return matchesUser && matchesKeyword
  }),
)

function formatCommittedAt(value: string) {
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false,
  }).format(new Date(value))
}
</script>

<template>
  <div class="page-stack logs-page">
    <section class="page-intro">
      <div>
        <p class="section-kicker">SUBMISSION LOGS</p>
        <h2>提交日志</h2>
        <p>集中查看每位成员的代码提交时间、所属区域和内容简述。</p>
      </div>
      <div class="log-total">
        <el-icon><Document /></el-icon>
        <span><strong>{{ filteredLogs.length }}</strong> 条记录</span>
      </div>
    </section>

    <section class="log-toolbar" aria-label="提交日志筛选">
      <el-input v-model="keyword" :prefix-icon="Search" clearable placeholder="搜索提交、区域或简述" />
      <el-select v-model="selectedUser" aria-label="选择用户">
        <el-option label="全部用户" value="all" />
        <el-option v-for="user in users" :key="user" :label="user" :value="user" />
      </el-select>
    </section>

    <section class="submission-table-wrap">
      <el-table :data="filteredLogs" class="submission-table" empty-text="暂无提交记录">
        <el-table-column label="提交人" width="170">
          <template #default="scope">
            <div class="log-user">
              <span class="user-avatar user-avatar-small">{{ scope.row.username.slice(0, 1).toUpperCase() }}</span>
              <strong>{{ scope.row.username }}</strong>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="220">
          <template #default="scope">{{ formatCommittedAt(scope.row.committedAt) }}</template>
        </el-table-column>
        <el-table-column label="提交" width="120">
          <template #default="scope">
            <code class="commit-code">{{ scope.row.commit }}</code>
          </template>
        </el-table-column>
        <el-table-column prop="area" label="所属区域" width="140">
          <template #default="scope">
            <el-tag effect="plain" type="info">{{ scope.row.area }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="summary" label="提交简述" min-width="320" />
      </el-table>
    </section>
  </div>
</template>
