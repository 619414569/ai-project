import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import AppLayout from '@/layouts/AppLayout.vue'
import DashboardView from '@/views/DashboardView.vue'
import IntegrationView from '@/views/IntegrationView.vue'
import LoginView from '@/views/LoginView.vue'

const router = createRouter({
  history: createWebHistory(),
  scrollBehavior(to) {
    if (to.hash) return { el: to.hash, top: 18, behavior: 'smooth' }
    return { top: 0 }
  },
  routes: [
    {
      path: '/login',
      name: 'login',
      component: LoginView,
      meta: { title: '登录' },
    },
    {
      path: '/',
      component: AppLayout,
      meta: { requiresAuth: true },
      children: [
        {
          path: '',
          name: 'dashboard',
          component: DashboardView,
          meta: { title: '工作台', requiresAuth: true },
        },
        {
          path: 'applications/personal',
          redirect: { name: 'dashboard', hash: '#my-applications' },
        },
        {
          path: 'applications/collaboration',
          redirect: { name: 'dashboard', hash: '#collaboration-applications' },
        },
        {
          path: 'submission-logs',
          redirect: { name: 'dashboard', hash: '#submission-logs' },
        },
        {
          path: 'integrations',
          name: 'integrations',
          component: IntegrationView,
          meta: { title: '组件联调', requiresAuth: true },
        },
      ],
    },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
})

router.beforeEach(async (to) => {
  const authStore = useAuthStore()
  if (authStore.token && !authStore.profile) {
    await authStore.restore()
  }
  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.name === 'login' && authStore.isAuthenticated) {
    return { name: 'dashboard' }
  }
  document.title = `${String(to.meta.title ?? '控制台')} - AI Project`
  return true
})

export default router
