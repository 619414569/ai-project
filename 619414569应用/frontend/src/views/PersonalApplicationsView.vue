<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Plus, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import ApplicationCard from '@/components/ApplicationCard.vue'
import ApplicationCreateDialog from '@/components/ApplicationCreateDialog.vue'
import { useApplicationStore } from '@/stores/applications'
import { useAuthStore } from '@/stores/auth'
import type { CreateApplicationInput, WorkspaceApplication } from '@/types/workspace'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const applicationStore = useApplicationStore()
const createDialogVisible = ref(false)
const keyword = ref('')
const statusFilter = ref<'all' | 'running' | 'paused'>('all')
const currentUser = computed(() => authStore.profile?.username ?? '619414569')

const filteredApplications = computed(() =>
  applicationStore.applications.filter((application) => {
    if (application.scope !== 'personal' || application.owner !== currentUser.value) return false
    const matchesKeyword = `${application.name}${application.description}${application.category}`
      .toLowerCase()
      .includes(keyword.value.trim().toLowerCase())
    const matchesStatus = statusFilter.value === 'all' || application.status === statusFilter.value
    return matchesKeyword && matchesStatus
  }),
)

function createApplication(input: CreateApplicationInput) {
  applicationStore.createApplication(input)
  ElMessage.success(`已创建“${input.name}”`)
}

function toggleStatus(application: WorkspaceApplication) {
  const nextStatus = application.status === 'running' ? 'paused' : 'running'
  applicationStore.updateApplication(application.id, { status: nextStatus })
  ElMessage.success(nextStatus === 'running' ? '应用已恢复' : '应用已暂停')
}

function toggleVisibility(application: WorkspaceApplication) {
  const nextVisibility = application.visibility === 'shared' ? 'exclusive' : 'shared'
  applicationStore.updateApplication(application.id, { visibility: nextVisibility })
  ElMessage.success(nextVisibility === 'shared' ? '应用已设为共享' : '应用已设为独享')
}

async function removeApplication(application: WorkspaceApplication) {
  const confirmed = await ElMessageBox.confirm(`删除后将无法继续访问“${application.name}”。`, '删除应用', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(() => true)
    .catch(() => false)
  if (!confirmed) return
  applicationStore.removeApplication(application.id)
  ElMessage.success('应用已删除')
}

function openApplication(application: WorkspaceApplication) {
  ElMessage.success(`正在打开“${application.name}”`)
}

onMounted(() => {
  if (route.query.action === 'create') {
    createDialogVisible.value = true
    void router.replace({ query: {} })
  }
})
</script>

<template>
  <div class="page-stack application-page">
    <section class="page-intro">
      <div>
        <p class="section-kicker">MY APPLICATIONS</p>
        <h2>个人应用</h2>
        <p>管理仅由你维护的应用，并控制共享范围和运行状态。</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="createDialogVisible = true">创建应用</el-button>
    </section>

    <section class="application-toolbar" aria-label="个人应用筛选">
      <el-input v-model="keyword" :prefix-icon="Search" clearable placeholder="搜索应用名称或类型" />
      <el-radio-group v-model="statusFilter">
        <el-radio-button value="all">全部</el-radio-button>
        <el-radio-button value="running">运行中</el-radio-button>
        <el-radio-button value="paused">已暂停</el-radio-button>
      </el-radio-group>
      <span class="toolbar-count">{{ filteredApplications.length }} 个应用</span>
    </section>

    <section v-if="filteredApplications.length" class="application-grid" aria-label="个人应用列表">
      <ApplicationCard
        v-for="application in filteredApplications"
        :key="application.id"
        :application="application"
        :current-user="currentUser"
        @open="openApplication"
        @toggle-status="toggleStatus"
        @toggle-visibility="toggleVisibility"
        @remove="removeApplication"
      />
    </section>
    <el-empty v-else description="没有符合条件的个人应用">
      <el-button type="primary" :icon="Plus" @click="createDialogVisible = true">创建第一个应用</el-button>
    </el-empty>

    <ApplicationCreateDialog
      v-model="createDialogVisible"
      scope="personal"
      :owner="currentUser"
      :available-collaborators="applicationStore.users"
      @create="createApplication"
    />
  </div>
</template>
