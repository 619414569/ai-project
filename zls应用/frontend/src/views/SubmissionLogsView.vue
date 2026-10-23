<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import DOMPurify from 'dompurify'
import { marked } from 'marked'
import { Document, Search, View } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { workspaceApi } from '@/api/modules/workspace'
import { useApplicationStore } from '@/stores/applications'
import type { UpdateLogDocument, UpdateLogSummary } from '@/types/workspace'
import { filterAndSortUpdateLogs } from '@/utils/update-log'

const activeLogType = ref('updates')
const submissionKeyword = ref('')
const selectedSubmissionUser = ref('all')
const updateKeyword = ref('')
const selectedUpdateAuthor = ref('all')
const updateLogs = ref<UpdateLogSummary[]>([])
const updateLogsLoading = ref(false)
const updateLogsError = ref('')
const previewVisible = ref(false)
const previewLoading = ref(false)
const previewDocument = ref<UpdateLogDocument | null>(null)

const applicationStore = useApplicationStore()
const submissionLogs = computed(() => applicationStore.submissionLogs)
const submissionUsers = computed(() => [...new Set(submissionLogs.value.map((log) => log.username))])
const updateAuthors = computed(() => [...new Set(updateLogs.value.map((log) => log.author))])

const filteredSubmissionLogs = computed(() =>
  submissionLogs.value.filter((log) => {
    const matchesUser = selectedSubmissionUser.value === 'all' || log.username === selectedSubmissionUser.value
    const matchesKeyword = `${log.summary}${log.area}${log.commit}${log.username}`
      .toLowerCase()
      .includes(submissionKeyword.value.trim().toLowerCase())
    return matchesUser && matchesKeyword
  }),
)
const filteredUpdateLogs = computed(() =>
  filterAndSortUpdateLogs(updateLogs.value, selectedUpdateAuthor.value, updateKeyword.value),
)
const previewHtml = computed(() => {
  if (!previewDocument.value) return ''
  const rendered = marked.parse(previewDocument.value.markdown, { async: false }) as string
  return DOMPurify.sanitize(rendered)
})

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

async function loadUpdateLogs() {
  updateLogsLoading.value = true
  updateLogsError.value = ''
  try {
    updateLogs.value = await workspaceApi.listUpdateLogs()
  } catch (error) {
    updateLogsError.value = error instanceof Error ? error.message : '版本日志加载失败'
  } finally {
    updateLogsLoading.value = false
  }
}

async function openUpdateLog(log: UpdateLogSummary) {
  previewVisible.value = true
  previewLoading.value = true
  previewDocument.value = null
  try {
    previewDocument.value = await workspaceApi.readUpdateLog(log.author, log.fileName)
  } catch (error) {
    previewVisible.value = false
    ElMessage.error(error instanceof Error ? error.message : '版本日志预览失败')
  } finally {
    previewLoading.value = false
  }
}

onMounted(loadUpdateLogs)
</script>

<template>
  <div class="page-stack logs-page">
    <section class="page-intro">
      <div>
        <p class="section-kicker">SUBMISSION LOGS</p>
        <h2>提交日志</h2>
        <p>查询成员版本更新与代码提交记录，并在线预览 Markdown 更新日志。</p>
      </div>
      <div class="log-total">
        <el-icon><Document /></el-icon>
        <span><strong>{{ activeLogType === 'updates' ? filteredUpdateLogs.length : filteredSubmissionLogs.length }}</strong> 条记录</span>
      </div>
    </section>

    <el-tabs v-model="activeLogType" class="log-tabs">
      <el-tab-pane label="版本更新日志" name="updates">
        <section class="log-toolbar" aria-label="版本更新日志筛选">
          <el-input v-model="updateKeyword" :prefix-icon="Search" clearable placeholder="搜索人员、版本、文件名或摘要" />
          <el-select v-model="selectedUpdateAuthor" aria-label="选择更新人员">
            <el-option label="全部人员" value="all" />
            <el-option v-for="author in updateAuthors" :key="author" :label="author" :value="author" />
          </el-select>
        </section>

        <el-alert
          v-if="updateLogsError"
          :title="updateLogsError"
          type="error"
          :closable="false"
          show-icon
          class="update-log-error"
        >
          <template #default><el-button link type="primary" @click="loadUpdateLogs">重新加载</el-button></template>
        </el-alert>

        <section class="submission-table-wrap update-log-table-wrap">
          <el-table
            v-loading="updateLogsLoading"
            :data="filteredUpdateLogs"
            class="submission-table update-log-table"
            empty-text="暂无版本更新日志"
          >
            <el-table-column label="更新人员" width="160">
              <template #default="scope">
                <div class="log-user">
                  <span class="user-avatar user-avatar-small">{{ scope.row.author.slice(0, 1).toUpperCase() }}</span>
                  <strong>{{ scope.row.author }}</strong>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="releasedAt" label="更新日期" width="130" />
            <el-table-column label="版本" width="100">
              <template #default="scope"><strong>{{ scope.row.version }}</strong></template>
            </el-table-column>
            <el-table-column label="类型" width="100">
              <template #default="scope">
                <el-tag effect="plain" :type="scope.row.releaseType === 'major' ? 'danger' : 'success'">
                  {{ scope.row.releaseType === 'major' ? '大版本' : '小版本' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="日志文件" min-width="230">
              <template #default="scope">
                <el-button link type="primary" :icon="View" @click="openUpdateLog(scope.row)">
                  {{ scope.row.fileName }}
                </el-button>
              </template>
            </el-table-column>
            <el-table-column prop="summary" label="更新简述" min-width="320" />
          </el-table>
        </section>
      </el-tab-pane>

      <el-tab-pane label="代码提交记录" name="submissions">
        <section class="log-toolbar" aria-label="代码提交日志筛选">
          <el-input v-model="submissionKeyword" :prefix-icon="Search" clearable placeholder="搜索人员、提交、区域或简述" />
          <el-select v-model="selectedSubmissionUser" aria-label="选择提交人">
            <el-option label="全部用户" value="all" />
            <el-option v-for="user in submissionUsers" :key="user" :label="user" :value="user" />
          </el-select>
        </section>

        <section class="submission-table-wrap">
          <el-table :data="filteredSubmissionLogs" class="submission-table" empty-text="暂无提交记录">
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
              <template #default="scope"><code class="commit-code">{{ scope.row.commit }}</code></template>
            </el-table-column>
            <el-table-column prop="area" label="所属区域" width="140">
              <template #default="scope"><el-tag effect="plain" type="info">{{ scope.row.area }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="summary" label="提交简述" min-width="320" />
          </el-table>
        </section>
      </el-tab-pane>
    </el-tabs>

    <el-drawer
      v-model="previewVisible"
      :title="previewDocument?.fileName ?? '版本更新日志'"
      size="min(760px, 96vw)"
      destroy-on-close
      class="update-log-drawer"
    >
      <div v-loading="previewLoading" class="update-log-preview-wrap">
        <div v-if="previewDocument" class="update-log-preview-meta">
          <el-tag effect="plain">{{ previewDocument.version }}</el-tag>
          <span>{{ previewDocument.author }}</span>
          <span>{{ previewDocument.releasedAt }}</span>
        </div>
        <article v-if="previewDocument" class="markdown-preview" v-html="previewHtml" />
      </div>
    </el-drawer>
  </div>
</template>
