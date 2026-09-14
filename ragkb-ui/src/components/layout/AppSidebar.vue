<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import {
  ChatDotRound,
  Document,
  FolderOpened,
  Menu,
  OfficeBuilding,
  User,
} from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const props = defineProps<{ collapsed: boolean }>()
const emit = defineEmits<{ 'update:collapsed': [value: boolean] }>()

const route = useRoute()
const auth = useAuthStore()

const items = computed(() => [
  { label: 'Assistente', to: '/chat', icon: ChatDotRound, visible: true },
  {
    label: 'Documentos',
    to: '/documents',
    icon: FolderOpened,
    visible: auth.hasAnyRole(['ADMIN', 'EDITOR']),
  },
  { label: 'Conflitos', to: '/conflicts', icon: Document, visible: auth.hasRole('ADMIN') },
  { label: 'Setores', to: '/sectors', icon: OfficeBuilding, visible: auth.hasRole('ADMIN') },
  { label: 'Usuarios', to: '/users', icon: User, visible: auth.hasRole('ADMIN') },
])
</script>

<template>
  <el-aside :width="collapsed ? '76px' : '250px'" class="sidebar">
    <div class="brand">
      <div class="brand-mark">R</div>
      <span v-show="!collapsed">rag<span>kb</span></span>
    </div>
    <div v-show="!collapsed" class="workspace-label">BASE DE CONHECIMENTO</div>
    <el-menu
      :default-active="route.path"
      :collapse="collapsed"
      :collapse-transition="false"
      router
      class="nav-menu"
    >
      <el-menu-item
        v-for="item in items.filter((entry) => entry.visible)"
        :key="item.to"
        :index="item.to"
      >
        <el-icon><component :is="item.icon" /></el-icon>
        <template #title>{{ item.label }}</template>
      </el-menu-item>
    </el-menu>
    <div class="sidebar-footer">
      <el-button text class="collapse-button" @click="emit('update:collapsed', !collapsed)">
        <el-icon><Menu /></el-icon>
        <span v-show="!collapsed">Recolher menu</span>
      </el-button>
    </div>
  </el-aside>
</template>
