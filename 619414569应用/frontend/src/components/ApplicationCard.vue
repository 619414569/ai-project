<script setup lang="ts">
import { computed } from 'vue'
import {
  ArrowRight,
  Connection,
  Delete,
  EditPen,
  Link,
  Lock,
  MoreFilled,
  Share,
  VideoPause,
  VideoPlay,
} from '@element-plus/icons-vue'
import type { WorkspaceApplication } from '@/types/workspace'

const props = withDefaults(
  defineProps<{
    application: WorkspaceApplication
    currentUser: string
    manageable?: boolean
  }>(),
  { manageable: true },
)

const emit = defineEmits<{
  open: [application: WorkspaceApplication]
  toggleStatus: [application: WorkspaceApplication]
  toggleVisibility: [application: WorkspaceApplication]
  remove: [application: WorkspaceApplication]
  editUrl: [application: WorkspaceApplication]
}>()

const isOwner = computed(() => props.application.owner === props.currentUser)
const categoryTone = computed(() => {
  const tones: Record<string, string> = {
    智能问答: 'green',
    数据分析: 'blue',
    内容生成: 'amber',
    流程自动化: 'red',
    知识管理: 'violet',
  }
  return tones[props.application.category] ?? 'green'
})

function formatUpdatedAt(value: string) {
  const date = new Date(value)
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(date)
}

function handleCommand(command: string) {
  if (command === 'visibility') emit('toggleVisibility', props.application)
  if (command === 'edit-url') emit('editUrl', props.application)
  if (command === 'remove') emit('remove', props.application)
}
</script>

<template>
  <article class="application-card" :class="{ 'is-paused': application.status === 'paused' }">
    <div class="application-card-topline">
      <span class="application-icon" :class="`tone-${categoryTone}`">
        <el-icon><Connection /></el-icon>
      </span>
      <div v-if="manageable" class="application-card-actions">
        <el-tooltip :content="application.status === 'running' ? '暂停应用' : '恢复应用'" placement="top">
          <el-button
            :icon="application.status === 'running' ? VideoPause : VideoPlay"
            circle
            text
            :aria-label="application.status === 'running' ? '暂停应用' : '恢复应用'"
            @click="emit('toggleStatus', application)"
          />
        </el-tooltip>
        <el-dropdown trigger="click" @command="handleCommand">
          <el-button :icon="MoreFilled" circle text aria-label="更多应用操作" />
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="edit-url" :icon="EditPen">
                {{ application.applicationUrl ? '修改应用地址' : '补填应用地址' }}
              </el-dropdown-item>
              <el-dropdown-item command="visibility" :icon="application.visibility === 'shared' ? Lock : Share">
                设为{{ application.visibility === 'shared' ? '独享' : '共享' }}
              </el-dropdown-item>
              <el-dropdown-item command="remove" :icon="Delete" divided class="dropdown-danger">
                删除应用
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <div class="application-card-title">
      <div>
        <h3>{{ application.name }}</h3>
        <span>{{ application.category }}</span>
      </div>
      <el-tag :type="application.status === 'running' ? 'success' : 'info'" effect="plain" size="small">
        {{ application.status === 'running' ? '运行中' : '已暂停' }}
      </el-tag>
    </div>

    <p class="application-description">{{ application.description }}</p>

    <div v-if="application.scope === 'collaboration'" class="application-members">
      <div class="avatar-stack" aria-label="合作成员">
        <span v-for="member in application.collaborators.slice(0, 3)" :key="member" class="mini-avatar">
          {{ member.slice(0, 1).toUpperCase() }}
        </span>
        <span v-if="application.collaborators.length > 3" class="member-overflow">
          +{{ application.collaborators.length - 3 }}
        </span>
      </div>
      <span>{{ isOwner ? '我创建的' : `${application.owner} 创建` }}</span>
    </div>

    <footer class="application-card-footer">
      <div class="application-meta">
        <span>
          <el-icon><component :is="application.visibility === 'shared' ? Share : Lock" /></el-icon>
          {{ application.visibility === 'shared' ? '共享' : '独享' }}
        </span>
        <span>
          <el-icon><Link /></el-icon>
          {{ application.applicationUrl ? '已配置地址' : '暂无地址' }}
        </span>
        <span>更新于 {{ formatUpdatedAt(application.updatedAt) }}</span>
      </div>
      <el-button
        type="primary"
        text
        :icon="ArrowRight"
        :disabled="application.status === 'paused'"
        @click="emit('open', application)"
      >
        打开
      </el-button>
    </footer>
  </article>
</template>
