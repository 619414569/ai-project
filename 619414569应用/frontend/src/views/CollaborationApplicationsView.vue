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
const ownershipFilter = ref<'all' | 'owned' | 'joined'>('all')
const currentUser = computed(() => authStore.profile?.username ?? '619414569')

const filteredApplications = computed(() =>
  applicationStore.applications.filter((application) => {
    if (application.scope !== 'collaboration') return false
    const isOwned = application.owner === currentUser.value
    const isJoined = application.collaborators.includes(currentUser.value)
    const matchesOwnership =
      ownershipFilter.value === 'all' ||
      (ownershipFilter.value === 'owned' && isOwned) ||
      (ownershipFilter.value === 'joined' && isJoined && !isOwned)
    const matchesKeyword = `${application.name}${application.description}${application.category}${application.owner}`
      .toLowerCase()
      .includes(keyword.value.trim().toLowerCase())
    return matchesOwnership && matchesKeyword
  }),
)

function createApplication(input: CreateApplicationInput) {
  applicationStore.createApplication(input)
  ElMessage.success(`已创建合作应用“${input.name}”`)
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
  if (application.owner !== currentUser.value) {
    ElMessage.warning('只有应用创建人可以删除合作应用')
    return
  }
  const confirmed = await ElMessageBox.confirm(`删除后所有合作人将无法访问“${application.name}”。`, '删除合作应用', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(() => true)
    .catch(() => false)
  if (!confirmed) return
  applicationStore.removeApplication(application.id)
  ElMessage.success('合作应用已删除')
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
        <p class="section-kicker">COLLABORATION</p>
        <h2>合作应用</h2>
        <p>邀请成员共同建设应用，统一管理访问范围和运行状态。</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="createDialogVisible = true">创建合作应用</el-button>
    </section>

    <section class="application-toolbar" aria-label="合作应用筛选">
      <el-input v-model="keyword" :prefix-icon="Search" clearable placeholder="搜索应用、类型或创建人" />
      <el-radio-group v-model="ownershipFilter">
        <el-radio-button value="all">全部</el-radio-button>
        <el-radio-button value="owned">我创建的</el-radio-button>
        <el-radio-button value="joined">我参与的</el-radio-button>
      </el-radio-group>
      <span class="toolbar-count">{{ filteredApplications.length }} 个应用</span>
    </section>

    <section v-if="filteredApplications.length" class="application-grid" aria-label="合作应用列表">
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
    <el-empty v-else description="没有符合条件的合作应用">
      <el-button type="primary" :icon="Plus" @click="createDialogVisible = true">创建合作应用</el-button>
    </el-empty>

    <ApplicationCreateDialog
      v-model="createDialogVisible"
      scope="collaboration"
      :owner="currentUser"
      :available-collaborators="applicationStore.users"
      @create="createApplication"
    />
  </div>
</template>
