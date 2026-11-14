import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

export const THEME_OPTIONS = [
  { id: 'light', label: 'Padrão' },
  { id: 'dark', label: 'Escuro' },
  { id: 'graphite', label: 'Grafite' },
  { id: 'indigo', label: 'Índigo' },
] as const

export type ThemeMode = (typeof THEME_OPTIONS)[number]['id']
const THEME_KEY_PREFIX = 'ragkb-theme:'

export function isThemeMode(value: unknown): value is ThemeMode {
  return THEME_OPTIONS.some((theme) => theme.id === value)
}

function applyTheme(mode: ThemeMode) {
  document.documentElement.dataset.theme = mode
  document.documentElement.style.colorScheme = mode === 'dark' ? 'dark' : 'light'
}

export const useThemeStore = defineStore('theme', () => {
  const mode = ref<ThemeMode>('light')
  const username = ref('')
  const isDark = computed(() => mode.value === 'dark')

  function loadForUser(user: string) {
    username.value = user
    const saved: unknown = localStorage.getItem(`${THEME_KEY_PREFIX}${user}`)
    mode.value = isThemeMode(saved) ? saved : 'light'
    applyTheme(mode.value)
  }

  function setMode(next: unknown) {
    if (!isThemeMode(next)) return
    mode.value = next
    applyTheme(next)
    if (username.value) localStorage.setItem(`${THEME_KEY_PREFIX}${username.value}`, next)
  }

  function resetForGuest() {
    username.value = ''
    mode.value = 'light'
    applyTheme('light')
  }

  return { mode, isDark, loadForUser, setMode, resetForGuest }
})
