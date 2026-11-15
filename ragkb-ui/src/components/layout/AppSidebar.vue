<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import type { NavItem } from '@/types'
import { useThemeStore, type ThemeMode } from '@/stores/theme'

const props = defineProps<{
  collapsed: boolean
  mobileOpen?: boolean
  items: NavItem[]
}>()

const emit = defineEmits<{
  (e: 'toggle'): void
  (e: 'navigate'): void
}>()

const route = useRoute()
const theme = useThemeStore()
const themeLogos: Record<ThemeMode, string> = {
  light: '/padrao.png',
  dark: '/dark.png',
  graphite: '/grafite.png',
  indigo: '/indigo.png',
}
const logoSrc = computed(() => themeLogos[theme.mode])
const visibleItems = computed(() => props.items.filter((entry) => entry.visible))
const isCollapsed = computed(() => props.collapsed && !props.mobileOpen)
</script>

<template>
  <el-aside :width="isCollapsed ? '76px' : '250px'"
    :class="['sidebar', { 'is-collapsed': isCollapsed, 'mobile-open': mobileOpen }]">
    <div class="brand">
      <img class="brand-mark" :src="logoSrc" alt="" aria-hidden="true" />
      <span v-show="!collapsed">rag<span>kb</span></span>
    </div>

    <el-button circle class="sidebar-toggle" :aria-label="isCollapsed ? 'Expandir menu' : 'Recolher menu'"
      :aria-expanded="!isCollapsed" @click="emit('toggle')">
      <el-icon><ArrowRight v-if="isCollapsed" /><ArrowLeft v-else /></el-icon>
    </el-button>

    <div v-show="!isCollapsed" class="workspace-label">BASE DE CONHECIMENTO</div>

    <!-- Área rolável: com o menu crescendo, itens nunca mais são cortados -->
    <div :class="['nav-scroll', { 'is-collapsed': collapsed }]">
      <el-menu :default-active="route.path" :collapse="isCollapsed" :collapse-transition="false" router class="nav-menu"
        @select="emit('navigate')">
        <el-menu-item v-for="item in visibleItems" :key="item.to" :index="item.to">
          <el-icon>
            <component :is="item.icon" />
          </el-icon>
          <template #title>{{ item.label }}</template>
        </el-menu-item>
      </el-menu>
    </div>

  </el-aside>
</template>

<style scoped>
.sidebar {
  --sidebar-bg: #111827;
  --sidebar-text: #fff;
  --sidebar-brand-accent: #72e2bf;
  --sidebar-muted: #8490a7;
  --sidebar-item: #aeb8c8;
  --sidebar-hover: #202b3d;
  --sidebar-hover-text: #fff;
  --sidebar-active: #273b4b;
  --sidebar-active-text: #81e4c3;
  --sidebar-scroll: #273b4b;
  position: relative;
  display: flex;
  flex-direction: column;
  padding: 24px 12px;
  background: var(--sidebar-bg);
  transition: width 0.2s;
  overflow: visible;
  height: 100vh;
}

.sidebar.is-collapsed {
  padding-inline: 0;
}

.sidebar.is-collapsed .brand {
  justify-content: center;
  padding-inline: 0;
}

.brand {
  height: 46px;
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 0 10px;
  color: var(--sidebar-text);
  font-size: 25px;
  font-weight: 800;
  letter-spacing: -1.5px;
  white-space: nowrap;
}

.brand span span {
  color: var(--sidebar-brand-accent);
}

.brand-mark {
  width: 30px;
  height: 30px;
  flex: 0 0 30px;
  border-radius: 8px;
  object-fit: contain;
}

.workspace-label {
  padding: 30px 12px 11px;
  color: var(--sidebar-muted);
  font: 10px 'DM Mono';
  letter-spacing: 1.2px;
  white-space: nowrap;
}

/* Nova regra: área de navegação rola sozinha; footer fica fixo via margin-top:auto */
.nav-scroll {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  /* esconde a barra quando não precisa, mas mantém rolagem disponível */
  scrollbar-width: thin;
  scrollbar-color: var(--sidebar-scroll) transparent;
}

.nav-scroll.is-collapsed {
  scrollbar-gutter: stable;
}

.nav-scroll::-webkit-scrollbar {
  width: 6px;
}

.nav-scroll::-webkit-scrollbar-thumb {
  background: var(--sidebar-scroll);
  border-radius: 3px;
}

.nav-menu {
  border: 0 !important;
  background: transparent !important;
}

.nav-menu :deep(.el-menu-item) {
  margin: 4px 0;
  border-radius: 9px;
  color: var(--sidebar-item);
  font-weight: 600;
}

.nav-menu :deep(.el-menu-item:hover) {
  background: var(--sidebar-hover);
  color: var(--sidebar-hover-text);
}

.nav-menu :deep(.el-menu-item.is-active) {
  background: var(--sidebar-active);
  color: var(--sidebar-active-text);
}

.nav-menu :deep(.el-menu-item.is-active::before) {
  content: '';
  position: absolute;
  left: 0;
  width: 3px;
  height: 24px;
  border-radius: 3px;
  background: var(--sidebar-brand-accent);
}

.sidebar-toggle {
  position: absolute;
  top: 35px;
  right: -12px;
  z-index: 2201;
  width: 24px;
  height: 24px;
  padding: 0;
  border: 1px solid var(--app-border);
  background: var(--app-surface);
  color: var(--app-text-secondary);
  box-shadow: 0 2px 8px rgb(15 23 42 / 28%);
}

.sidebar-toggle:hover {
  border-color: var(--app-focus);
  background: var(--app-hover);
  color: var(--app-accent);
}

.sidebar-toggle:focus-visible {
  outline: 2px solid var(--app-focus);
  outline-offset: 2px;
}

@media (max-width: 760px) {
  .sidebar {
    display: none;
  }

  .sidebar.mobile-open {
    position: fixed;
    z-index: 2000;
    inset: 0 auto 0 0;
    display: flex;
    width: min(280px, calc(100vw - 48px)) !important;
    height: 100dvh;
    padding: 20px 12px;
    box-shadow: 10px 0 32px rgba(15, 23, 42, 0.2);
  }

  .sidebar.mobile-open.is-collapsed {
    padding-inline: 12px;
  }
}
</style>
