import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { authService } from '../services/auth.service'

type Session = { username: string; roles: string[]; expiresAt: number }

// localStorage (não sessionStorage): o link de fonte do artigo abre em
// nova aba — sessionStorage não atravessa abas e o guard mandaria o
// usuário para /login. logout() limpa as chaves.
const SESSION_KEY = 'ragkb-session'
const TOKEN_KEY = 'ragkb-token'

const saved = () => {
  try {
    return JSON.parse(localStorage.getItem(SESSION_KEY) || 'null') as Session | null
  } catch {
    return null
  }
}

function tokenPayload(token: string): { exp?: number; roles?: string[] } {
  try {
    const parts = token.split('.')
    // JWT tem 3 segmentos; o payload é o índice 1. Com noUncheckedIndexedAccess,
    // o acesso por índice é string | undefined — valida antes de decodificar.
    const payloadSegment = parts[1]
    if (!payloadSegment) return {}
    const decoded = atob(payloadSegment.replace(/-/g, '+').replace(/_/g, '/'))
    const parsed: unknown = JSON.parse(decoded)
    if (parsed && typeof parsed === 'object') {
      const obj = parsed as { exp?: number; roles?: unknown }
      return {
        exp: typeof obj.exp === 'number' ? obj.exp : undefined,
        roles: Array.isArray(obj.roles) ? (obj.roles as string[]) : [],
      }
    }
    return {}
  } catch {
    return {}
  }
}

export const useAuthStore = defineStore('auth', () => {
  const session = ref<Session | null>(saved())

  const isAuthenticated = computed(() => !!session.value && session.value.expiresAt > Date.now())
  const username = computed(() => session.value?.username || '')
  const roles = computed(() => session.value?.roles || [])
  const initials = computed(
    () =>
      username.value
        .split(/[._\s]/)
        .map((x) => x[0])
        .join('')
        .slice(0, 2)
        .toUpperCase() || 'U',
  )

  async function login(user: string, password: string) {
    const response = await authService.login(user, password)
    const token = response.token || response.accessToken
    if (!token) throw new Error('O backend não retornou o token de acesso.')
    const payload = tokenPayload(token)
    const newSession = {
      username: user,
      roles: Array.isArray(payload.roles) ? payload.roles : [],
      expiresAt: payload.exp
        ? payload.exp * 1000
        : Date.now() + (response.expiresIn || 3600) * 1000,
    }
    session.value = newSession
    localStorage.setItem(SESSION_KEY, JSON.stringify(newSession))
    localStorage.setItem(TOKEN_KEY, token)
  }

  function logout() {
    session.value = null
    localStorage.removeItem(SESSION_KEY)
    localStorage.removeItem(TOKEN_KEY)
  }

  function hasRole(role: string) {
    return roles.value.includes(role)
  }

  function hasAnyRole(check: string[]) {
    return check.some(hasRole)
  }

  return { isAuthenticated, username, roles, initials, login, logout, hasRole, hasAnyRole }
})
