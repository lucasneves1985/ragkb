import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import LoginView from '../views/LoginView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', redirect: '/chat' },
    { path: '/login', component: LoginView },
    { path: '/chat', component: () => import('../views/ChatView.vue'), meta: { requiresAuth: true } },
    {
      path: '/documents',
      component: () => import('../views/DocumentsView.vue'),
      meta: { requiresAuth: true, roles: ['ADMIN', 'EDITOR'] },
    },
    {
      path: '/conflicts',
      component: () => import('../views/ConflictsView.vue'),
      meta: { requiresAuth: true, roles: ['ADMIN'] },
    },
    {
      path: '/users',
      component: () => import('../views/UsersView.vue'),
      meta: { requiresAuth: true, roles: ['ADMIN'] }
    },
    {
      path: '/forbidden',
      component: () => import('../views/ForbiddenView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/sectors',
      component: () => import('../views/SectorView.vue'),
      meta: { requiresAuth: true, roles: ['ADMIN'] }
    },
  ],
})
router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.requiresAuth && !auth.isAuthenticated) return '/login'
  const roles = to.meta.roles as string[] | undefined
  if (roles && !auth.hasAnyRole(roles)) return '/forbidden'
  if (to.path === '/login' && auth.isAuthenticated) return '/chat'
})

export default router
