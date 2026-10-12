import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { workspaceApi } from '@/api/modules/workspace'
import type { CreateApplicationInput, SubmissionLog, WorkspaceApplication } from '@/types/workspace'

export const useApplicationStore = defineStore('applications', () => {
  const applications = ref<WorkspaceApplication[]>([])
  const users = ref<string[]>([])
  const submissionLogs = ref<SubmissionLog[]>([])
  const loading = ref(false)
  let loadPromise: Promise<void> | null = null

  const runningCount = computed(() => applications.value.filter((item) => item.status === 'running').length)
  const sharedCount = computed(() => applications.value.filter((item) => item.visibility === 'shared').length)

  async function loadWorkspace() {
    if (loadPromise) return loadPromise
    loading.value = true
    loadPromise = Promise.all([
      workspaceApi.listApplications(),
      workspaceApi.listUsers(),
      workspaceApi.listSubmissionLogs(),
    ])
      .then(([applicationList, userList, logs]) => {
        applications.value = applicationList
        users.value = userList
        submissionLogs.value = logs
      })
      .finally(() => {
        loading.value = false
        loadPromise = null
      })
    return loadPromise
  }

  async function createApplication(input: CreateApplicationInput) {
    const request = {
      name: input.name,
      description: input.description,
      category: input.category,
      scope: input.scope,
      visibility: input.visibility,
      collaborators: input.collaborators,
      applicationUrl: input.applicationUrl,
    }
    const application = await workspaceApi.createApplication(request)
    applications.value.unshift(application)
    return application
  }

  async function updateApplication(id: string, changes: Partial<WorkspaceApplication>) {
    const application = await workspaceApi.updateApplication(id, changes)
    const index = applications.value.findIndex((item) => item.id === id)
    if (index >= 0) applications.value[index] = application
    return application
  }

  async function removeApplication(id: string) {
    await workspaceApi.removeApplication(id)
    applications.value = applications.value.filter((item) => item.id !== id)
  }

  return {
    applications,
    users,
    submissionLogs,
    loading,
    runningCount,
    sharedCount,
    loadWorkspace,
    createApplication,
    updateApplication,
    removeApplication,
  }
})
