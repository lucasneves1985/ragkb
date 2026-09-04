import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import LoginView from '../views/LoginView.vue'
import ChatView from '../views/ChatView.vue'
import DocumentsView from '../views/DocumentsView.vue'
import ConflictsView from '../views/ConflictsView.vue'
import UsersView from '../views/UsersView.vue'
import ForbiddenView from '../views/ForbiddenView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', redirect: '/chat' }, { path: '/login', component: LoginView }, { path: '/chat', component: ChatView, meta: { requiresAuth: true } },
    { path: '/documents', component: DocumentsView, meta: { requiresAuth: true, roles: ['ADMIN', 'EDITOR'] } }, { path: '/conflicts', component: ConflictsView, meta: { requiresAuth: true, roles: ['ADMIN'] } },
    { path: '/users', component: UsersView, meta: { requiresAuth: true, roles: ['ADMIN'] } }, { path: '/forbidden', component: ForbiddenView, meta: { requiresAuth: true } },
  ],
})
router.beforeEach((to) => { const auth = useAuthStore(); if (to.meta.requiresAuth && !auth.isAuthenticated) return '/login'; const roles = to.meta.roles as string[] | undefined; if (roles && !auth.hasAnyRole(roles)) return '/forbidden'; if (to.path === '/login' && auth.isAuthenticated) return '/chat' })

export default router
