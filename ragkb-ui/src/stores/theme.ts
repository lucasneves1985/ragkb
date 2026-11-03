import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

export type ThemeMode = 'light' | 'dark'
const THEME_KEY_PREFIX = 'ragkb-theme:'

function applyTheme(mode: ThemeMode) {
  document.documentElement.dataset.theme = mode
  document.documentElement.style.colorScheme = mode
}

export const useThemeStore = defineStore('theme', () => {
  const mode = ref<ThemeMode>('light')
  const username = ref('')
  const isDark = computed(() => mode.value === 'dark')

  function loadForUser(user: string) {
    username.value = user
    const saved = localStorage.getItem(`${THEME_KEY_PREFIX}${user}`)
    mode.value = saved === 'dark' ? 'dark' : 'light'
    applyTheme(mode.value)
  }

  function setMode(next: ThemeMode) {
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
