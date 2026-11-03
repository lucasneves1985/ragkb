<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { Menu } from '@element-plus/icons-vue'
import type { NavItem } from '@/types'

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
const visibleItems = computed(() => props.items.filter((entry) => entry.visible))
const isCollapsed = computed(() => props.collapsed && !props.mobileOpen)
</script>

<template>
  <el-aside :width="isCollapsed ? '76px' : '250px'"
    :class="['sidebar', { 'is-collapsed': isCollapsed, 'mobile-open': mobileOpen }]">
    <div class="brand">
      <div class="brand-mark">R</div>
      <span v-show="!collapsed">rag<span>kb</span></span>
    </div>

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

    <div class="sidebar-footer">
      <el-button text class="collapse-button" @click="emit('toggle')">
        <el-icon>
          <Menu />
        </el-icon>
        <span v-show="!isCollapsed">Recolher menu</span>
      </el-button>
    </div>
  </el-aside>
</template>

<style scoped>
.sidebar {
  position: relative;
  display: flex;
  flex-direction: column;
  padding: 24px 12px;
  background: #111827;
  transition: width 0.2s;
  overflow: hidden;
  height: 100vh;
}

.sidebar.is-collapsed {
  padding-inline: 0;
}

.brand {
  height: 46px;
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 0 10px;
  color: #fff;
  font-size: 25px;
  font-weight: 800;
  letter-spacing: -1.5px;
  white-space: nowrap;
}

.brand span span {
  color: #72e2bf;
}

.brand-mark {
  display: grid;
  place-items: center;
  width: 30px;
  height: 30px;
  border-radius: 9px;
  color: #102219;
  background: #72e2bf;
  font: 700 17px 'DM Mono';
}

.workspace-label {
  padding: 30px 12px 11px;
  color: #8490a7;
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
  scrollbar-color: #273b4b transparent;
}

.nav-scroll.is-collapsed {
  scrollbar-gutter: stable;
}

.nav-scroll::-webkit-scrollbar {
  width: 6px;
}

.nav-scroll::-webkit-scrollbar-thumb {
  background: #273b4b;
  border-radius: 3px;
}

.nav-menu {
  border: 0 !important;
  background: transparent !important;
}

.nav-menu :deep(.el-menu-item) {
  margin: 4px 0;
  border-radius: 9px;
  color: #aeb8c8;
  font-weight: 600;
}

.nav-menu :deep(.el-menu-item:hover) {
  background: #202b3d !important;
  color: #fff !important;
}

.nav-menu :deep(.el-menu-item.is-active) {
  background: #273b4b !important;
  color: #81e4c3 !important;
}

.nav-menu :deep(.el-menu-item.is-active::before) {
  content: '';
  position: absolute;
  left: 0;
  width: 3px;
  height: 24px;
  border-radius: 3px;
  background: #72e2bf;
}

.sidebar-footer {
  margin-top: auto;
}

.collapse-button {
  width: 100%;
  justify-content: flex-start;
  gap: 11px;
  color: #aeb8c8 !important;
  font-weight: 600;
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
