import { createRouter, createWebHistory } from 'vue-router'
import AppLayout from '@/layouts/AppLayout.vue'
import DashboardView from '@/views/DashboardView.vue'
import NotebookView from '@/views/NotebookView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'notebook',
      component: NotebookView,
      meta: { title: '手账' },
    },
    {
      path: '/status',
      component: AppLayout,
      children: [
        {
          path: '',
          name: 'dashboard',
          component: DashboardView,
          meta: { title: '运行概览' },
        },
      ],
    },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
})

router.beforeEach((to) => {
  document.title = `${String(to.meta.title ?? '控制台')} - Notebook`
  return true
})

export default router
