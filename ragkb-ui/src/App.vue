<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ChatDotRound,
  Document,
  FolderOpened,
  OfficeBuilding,
  User,
} from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import AppSidebar from '@/components/layout/AppSidebar.vue'
import AppTopbar from '@/components/layout/AppTopbar.vue'
import type { NavItem } from '@/types'


const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const collapsed = ref(false)
const loggedIn = computed(() => auth.isAuthenticated)

watch(loggedIn, (authed) => {
  if (authed && route.path === '/login') {
    router.replace('/chat')
  }
})

const items = computed<NavItem[]>(() => [
  { label: 'Assistente', to: '/chat', icon: ChatDotRound, visible: true },
  {
    label: 'Documentos',
    to: '/documents',
    icon: FolderOpened,
    visible: auth.hasAnyRole(['ADMIN', 'EDITOR']),
  },
  { label: 'Conflitos', to: '/conflicts', icon: Document, visible: auth.hasRole('ADMIN') },
  { label: 'Setores', to: '/sectors', icon: OfficeBuilding, visible: auth.hasRole('ADMIN') },
  { label: 'Usuários', to: '/users', icon: User, visible: auth.hasRole('ADMIN') },
])

function logout() {
  auth.logout()
  router.push('/login')
}
</script>

<template>
  <router-view v-if="!loggedIn" />

  <el-container v-else class="app-shell" direction="horizontal">
    <AppSidebar :collapsed="collapsed" :items="items" @toggle="collapsed = !collapsed" />

    <el-container direction="vertical">
      <AppTopbar
        :username="auth.username"
        :initials="auth.initials"
        :roles="auth.roles"
        @new-query="router.push('/chat')"
        @logout="logout"
      />

      <el-main class="content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style>
@import url('https://fonts.googleapis.com/css2?family=DM+Mono:wght@400;500&family=Manrope:wght@400;500;600;700;800&display=swap');

:root {
  font-family: Manrope, Arial, sans-serif;
  color: #172033;
  background: #f6f7fb;
}

* {
  box-sizing: border-box;
}

html,
body {
  margin: 0;
  height: 100%;
  overflow: hidden;
  min-width: 320px;
}

button,
input,
textarea {
  font-family: inherit;
}

.app-shell {
  height: 100vh;
  max-height: 100vh;
  overflow: hidden;
  background: #f6f7fb;
}

.content {
  padding: 34px 38px;
  overflow: auto;
  height: calc(100vh - 76px);
}

.page-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 28px;
}

.eyebrow {
  color: #4eb78f;
  font: 500 11px 'DM Mono';
  text-transform: uppercase;
  letter-spacing: 1px;
  margin-bottom: 8px;
}

.page-heading h1 {
  margin: 0;
  font-size: 27px;
  letter-spacing: -1px;
  color: #172033;
}

.page-heading p {
  margin: 7px 0 0;
  color: #718096;
  font-size: 13px;
}

.surface {
  background: #fff;
  border: 1px solid #e8ebf1;
  border-radius: 15px;
  box-shadow: 0 4px 16px rgba(22, 31, 48, 0.03);
}

.status-active {
  --el-tag-bg-color: #eaf9f2;
  --el-tag-border-color: #c0edda;
  --el-tag-text-color: #168463;
}

.status-archived {
  --el-tag-bg-color: #f2f4f7;
  --el-tag-border-color: #e2e7ed;
  --el-tag-text-color: #67758a;
}

.status-superseded {
  --el-tag-bg-color: #fff5e9;
  --el-tag-border-color: #f8deb4;
  --el-tag-text-color: #ba7012;
}

.muted {
  color: #8490a1;
  font-size: 12px;
}

.primary-button {
  --el-button-bg-color: #168463;
  --el-button-border-color: #168463;
  --el-button-text-color: #fff;
  --el-button-hover-text-color: #fff;
  --el-button-active-text-color: #fff;
  --el-button-hover-bg-color: #0f7253;
  --el-button-hover-border-color: #0f7253;
  color: #fff !important;
  font-weight: 700;
}

.primary-button span,
.primary-button .el-icon {
  color: #fff !important;
}

.el-table {
  --el-table-header-bg-color: #f9fafc;
  --el-table-border-color: #edf0f4;
  --el-table-row-hover-bg-color: #f7fbf9;
}

.el-table th.el-table__cell {
  color: #708095;
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.el-table .cell {
  color: #344054;
  font-size: 13px;
}

.el-dialog {
  border-radius: 16px;
}

.el-dialog__title {
  font-weight: 800;
  color: #182235;
}

@media (max-width: 760px) {
  .content {
    padding: 24px 16px;
  }

  .page-heading {
    display: block;
  }

  .page-heading .el-button {
    margin-top: 15px;
  }
}
</style>
