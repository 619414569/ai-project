<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Connection, Plus, User } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import ApplicationCard from '@/components/ApplicationCard.vue'
import ApplicationCreateDialog from '@/components/ApplicationCreateDialog.vue'
import { useApplicationStore } from '@/stores/applications'
import { useAuthStore } from '@/stores/auth'
import SubmissionLogsView from '@/views/SubmissionLogsView.vue'
import type { CreateApplicationInput, WorkspaceApplication } from '@/types/workspace'
import { normalizeApplicationUrl, validateApplicationUrl } from '@/utils/application-url'

const authStore = useAuthStore()
const applicationStore = useApplicationStore()
const personalDialogVisible = ref(false)
const collaborationDialogVisible = ref(false)
const currentUser = computed(() => authStore.profile?.username ?? '')

function showError(error: unknown, fallback: string) {
  ElMessage.error(error instanceof Error ? error.message : fallback)
}

function byLatestUpdate(left: WorkspaceApplication, right: WorkspaceApplication) {
  return new Date(right.updatedAt).getTime() - new Date(left.updatedAt).getTime()
}

const myPersonalApplications = computed(() =>
  applicationStore.applications
    .filter(
      (application) => application.scope === 'personal' && application.owner === currentUser.value,
    )
    .sort(byLatestUpdate),
)

const otherPersonalGroups = computed(() => {
  const groups = new Map<string, WorkspaceApplication[]>()
  applicationStore.applications
    .filter(
      (application) =>
        application.scope === 'personal' &&
        application.owner !== currentUser.value &&
        application.visibility === 'shared',
    )
    .forEach((application) => {
      const applications = groups.get(application.owner) ?? []
      applications.push(application)
      groups.set(application.owner, applications)
    })

  return [...groups.entries()]
    .sort(([left], [right]) => left.localeCompare(right))
    .map(([owner, applications]) => ({ owner, applications: applications.sort(byLatestUpdate) }))
})

const collaborationApplications = computed(() =>
  applicationStore.applications
    .filter(
      (application) =>
        application.scope === 'collaboration' &&
        (application.owner === currentUser.value || application.collaborators.includes(currentUser.value)),
    )
    .sort(byLatestUpdate),
)

const otherPersonalCount = computed(() =>
  otherPersonalGroups.value.reduce((total, group) => total + group.applications.length, 0),
)

async function createApplication(input: CreateApplicationInput) {
  try {
    await applicationStore.createApplication(input)
    ElMessage.success(
      input.scope === 'personal' ? `已创建“${input.name}”` : `已创建合作应用“${input.name}”`,
    )
  } catch (error) {
    showError(error, '应用创建失败')
  }
}

async function toggleStatus(application: WorkspaceApplication) {
  if (application.owner !== currentUser.value) return
  const nextStatus = application.status === 'running' ? 'paused' : 'running'
  try {
    await applicationStore.updateApplication(application.id, { status: nextStatus })
    ElMessage.success(nextStatus === 'running' ? '应用已恢复' : '应用已暂停')
  } catch (error) {
    showError(error, '应用状态更新失败')
  }
}

async function toggleVisibility(application: WorkspaceApplication) {
  if (application.owner !== currentUser.value) return
  const nextVisibility = application.visibility === 'shared' ? 'exclusive' : 'shared'
  try {
    await applicationStore.updateApplication(application.id, { visibility: nextVisibility })
    ElMessage.success(nextVisibility === 'shared' ? '应用已设为共享' : '应用已设为独享')
  } catch (error) {
    showError(error, '访问方式更新失败')
  }
}

async function removeApplication(application: WorkspaceApplication) {
  if (application.scope === 'collaboration' && application.owner !== currentUser.value) {
    ElMessage.warning('只有应用创建人可以删除合作应用')
    return
  }
  const message =
    application.scope === 'collaboration'
      ? `删除后所有合作人将无法访问“${application.name}”。`
      : `删除后将无法继续访问“${application.name}”。`
  const confirmed = await ElMessageBox.confirm(message, '删除应用', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(() => true)
    .catch(() => false)
  if (!confirmed) return
  try {
    await applicationStore.removeApplication(application.id)
    ElMessage.success('应用已删除')
  } catch (error) {
    showError(error, '应用删除失败')
  }
}

function navigateToApplication(application: WorkspaceApplication) {
  if (!application.applicationUrl) return
  window.location.assign(application.applicationUrl)
}

function canEditApplication(application: WorkspaceApplication) {
  return application.owner === currentUser.value
}

async function editApplicationUrl(application: WorkspaceApplication, openAfterSave = false) {
  if (!canEditApplication(application)) {
    ElMessage.warning('暂无地址，请联系应用创建人补填')
    return
  }

  const result = await ElMessageBox.prompt(
    application.applicationUrl ? '修改后将使用新地址打开应用。' : '暂无地址，请补填后再打开。',
    application.applicationUrl ? '修改应用地址' : '补填应用地址',
    {
      confirmButtonText: '保存',
      cancelButtonText: '取消',
      inputValue: application.applicationUrl ?? '',
      inputPlaceholder: 'https://app.example.com',
      inputValidator: (value) => {
        if (!value.trim()) return '请输入应用地址'
        return validateApplicationUrl(value)
      },
    },
  ).catch(() => null)

  if (!result) return
  const applicationUrl = normalizeApplicationUrl(result.value)
  if (!applicationUrl) {
    ElMessage.warning('应用地址不能为空')
    return
  }
  try {
    const updated = await applicationStore.updateApplication(application.id, { applicationUrl })
    ElMessage.success('应用地址已保存')
    if (openAfterSave) navigateToApplication(updated)
  } catch (error) {
    showError(error, '应用地址保存失败')
  }
}

function openApplication(application: WorkspaceApplication) {
  if (application.applicationUrl) {
    navigateToApplication(application)
    return
  }
  void editApplicationUrl(application, true)
}

onMounted(async () => {
  try {
    await applicationStore.loadWorkspace()
  } catch (error) {
    showError(error, '工作台数据加载失败')
  }
})
</script>

<template>
  <div v-loading="applicationStore.loading" class="workbench-page">
    <section id="my-applications" class="workbench-section workbench-section-first">
      <div class="workbench-section-heading">
        <div>
          <p class="section-kicker">MY APPLICATIONS</p>
          <h2>我的个人应用</h2>
          <p>登录后首先进入你的应用空间，在这里创建和管理个人应用。</p>
        </div>
        <div class="section-heading-actions">
          <span class="section-count">{{ myPersonalApplications.length }} 个应用</span>
          <el-button type="primary" :icon="Plus" @click="personalDialogVisible = true">
            创建个人应用
          </el-button>
        </div>
      </div>

      <div v-if="myPersonalApplications.length" class="application-grid" aria-label="我的个人应用">
        <ApplicationCard
          v-for="application in myPersonalApplications"
          :key="application.id"
          :application="application"
          :current-user="currentUser"
          :manageable="application.owner === currentUser"
          @open="openApplication"
          @toggle-status="toggleStatus"
          @toggle-visibility="toggleVisibility"
          @edit-url="editApplicationUrl"
          @remove="removeApplication"
        />
      </div>
      <el-empty v-else description="还没有个人应用">
        <el-button type="primary" :icon="Plus" @click="personalDialogVisible = true">
          创建第一个应用
        </el-button>
      </el-empty>
    </section>

    <section id="other-applications" class="workbench-section">
      <div class="workbench-section-heading">
        <div>
          <p class="section-kicker">TEAM APPLICATIONS</p>
          <h2>其他成员的个人应用</h2>
          <p>按创建人查看团队成员已共享的个人应用。</p>
        </div>
        <span class="section-count">{{ otherPersonalGroups.length }} 位成员 · {{ otherPersonalCount }} 个应用</span>
      </div>

      <div v-if="otherPersonalGroups.length" class="owner-groups">
        <section v-for="group in otherPersonalGroups" :key="group.owner" class="owner-group">
          <div class="owner-group-heading">
            <span class="owner-avatar">{{ group.owner.slice(0, 1).toUpperCase() }}</span>
            <div>
              <h3>{{ group.owner }}</h3>
              <p><el-icon><User /></el-icon>{{ group.applications.length }} 个共享应用</p>
            </div>
          </div>
          <div class="application-grid" :aria-label="`${group.owner} 的个人应用`">
            <ApplicationCard
              v-for="application in group.applications"
              :key="application.id"
              :application="application"
              :current-user="currentUser"
              :manageable="false"
              @open="openApplication"
            />
          </div>
        </section>
      </div>
      <el-empty v-else description="暂无其他成员共享的应用" />
    </section>

    <section id="collaboration-applications" class="workbench-section">
      <div class="workbench-section-heading">
        <div>
          <p class="section-kicker">COLLABORATION</p>
          <h2>合作应用</h2>
          <p>查看你创建或参与的合作应用，并统一管理合作成员和运行状态。</p>
        </div>
        <div class="section-heading-actions">
          <span class="section-count">{{ collaborationApplications.length }} 个应用</span>
          <el-button type="primary" :icon="Connection" @click="collaborationDialogVisible = true">
            创建合作应用
          </el-button>
        </div>
      </div>

      <div v-if="collaborationApplications.length" class="application-grid" aria-label="合作应用">
        <ApplicationCard
          v-for="application in collaborationApplications"
          :key="application.id"
          :application="application"
          :current-user="currentUser"
          :manageable="application.owner === currentUser"
          @open="openApplication"
          @toggle-status="toggleStatus"
          @toggle-visibility="toggleVisibility"
          @edit-url="editApplicationUrl"
          @remove="removeApplication"
        />
      </div>
      <el-empty v-else description="还没有合作应用">
        <el-button type="primary" :icon="Connection" @click="collaborationDialogVisible = true">
          创建合作应用
        </el-button>
      </el-empty>
    </section>

    <section id="submission-logs" class="workbench-section workbench-log-section">
      <SubmissionLogsView />
    </section>

    <ApplicationCreateDialog
      v-model="personalDialogVisible"
      scope="personal"
      :owner="currentUser"
      :available-collaborators="applicationStore.users"
      @create="createApplication"
    />
    <ApplicationCreateDialog
      v-model="collaborationDialogVisible"
      scope="collaboration"
      :owner="currentUser"
      :available-collaborators="applicationStore.users"
      @create="createApplication"
    />
  </div>
</template>
