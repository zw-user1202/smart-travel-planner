import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: () => import('@/views/Login.vue'), meta: { public: true } },
    { path: '/', name: 'workbench', component: () => import('@/views/Workbench.vue') },
    { path: '/trips/new', name: 'trip-new', component: () => import('@/views/TripCreate.vue') },
    { path: '/trips/:id', name: 'trip-detail', component: () => import('@/views/TripDetail.vue') },
    { path: '/routes', name: 'routes', component: () => import('@/views/Routes.vue') },
    { path: '/costs', name: 'costs', component: () => import('@/views/Costs.vue') },
    { path: '/footprint', name: 'footprint', component: () => import('@/views/Footprint.vue') },
    { path: '/checklist', name: 'checklist', component: () => import('@/views/Checklist.vue') },
    { path: '/admin', name: 'admin', component: () => import('@/views/Admin.vue') }
  ]
})

router.beforeEach(to => {
  const token = localStorage.getItem('token')
  if (!to.meta.public && !token) return '/login'
  if (to.path === '/login' && token) return '/'
})

export default router
