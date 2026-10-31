<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Bell,
  Lock,
  Postcard,
  ChatDotRound,
  Connection,
  Document,
  FolderOpened,
  List,
  Notebook,
  OfficeBuilding,
  Reading,
  User,
} from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { useQueryClient } from '@tanstack/vue-query'
import AppSidebar from '@/components/layout/AppSidebar.vue'
import AppTopbar from '@/components/layout/AppTopbar.vue'
import type { NavItem } from '@/types'
import { MY_PROFILE_KEY, useMyProfile } from '@/composables/useMyProfile'
import { resolveAvatarUrl } from '@/services/users.service'
import { useThemeStore } from '@/stores/theme'


const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const theme = useThemeStore()
const queryClient = useQueryClient()
const { profile } = useMyProfile()

const collapsed = ref(false)
const mobileNavOpen = ref(false)
const loggedIn = computed(() => auth.isAuthenticated)
const displayName = computed(() => profile.value?.fullName || auth.username)
const profileInitials = computed(() => {
  const initials = profile.value?.fullName
    ?.trim()
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0])
    .join('')
    .toUpperCase()
  return initials || auth.initials
})
const avatarUrl = computed(() => resolveAvatarUrl(profile.value?.avatarUrl))

watch(loggedIn, (authed) => {
  if (authed && route.path === '/login') {
    router.replace('/chat')
  }
})

watch([loggedIn, () => auth.username], ([authed, username]) => {
  if (authed && username) theme.loadForUser(username)
  else theme.resetForGuest()
}, { immediate: true })

const items = computed<NavItem[]>(() => [
  { label: 'Assistente', to: '/chat', icon: ChatDotRound, visible: true },
  {
    label: 'Portal',
    to: '/portal',
    icon: Reading, // importe de '@element-plus/icons-vue'
    visible: true, // todos os autenticados
  },
  { label: 'Cards', to: '/cards', icon: Postcard, visible: true },
  {
    label: 'Documentos',
    to: '/documents',
    icon: FolderOpened,
    visible: auth.hasAnyRole(['ADMIN', 'EDITOR']),
  },
  {
    label: 'Artigos',
    to: '/articles',
    icon: Notebook,
    visible: auth.hasAnyRole(['ADMIN', 'EDITOR']),
  },
  {
    label: 'Regras',
    to: '/business-rules',
    icon: List,
    visible: true,
  },

  { label: 'Conflitos', to: '/conflicts', icon: Document, visible: auth.hasRole('ADMIN') },
  { label: 'Setores', to: '/sectors', icon: OfficeBuilding, visible: auth.hasRole('ADMIN') },
  { label: 'Integrações', to: '/integrations', icon: Connection, visible: auth.hasAnyRole(['ADMIN', 'EDITOR']) },
  { label: 'Certificados SSL', to: '/certificates', icon: Lock, visible: true },
  { label: 'Lembretes', to: '/reminders', icon: Bell, visible: true },
  { label: 'WhatsApp', to: '/whatsapp', icon: ChatDotRound, visible: auth.hasRole('ADMIN') },
  { label: 'Usuários', to: '/users', icon: User, visible: auth.hasRole('ADMIN') },
])

function logout() {
  auth.logout()
  queryClient.removeQueries({ queryKey: MY_PROFILE_KEY })
  router.push('/login')
}
</script>

<template>
  <router-view v-if="!loggedIn" />

  <el-container v-else class="app-shell" direction="horizontal">
    <div v-if="mobileNavOpen" class="mobile-nav-backdrop" @click="mobileNavOpen = false"></div>
    <AppSidebar :collapsed="collapsed" :mobile-open="mobileNavOpen" :items="items"
      @toggle="collapsed = !collapsed" @navigate="mobileNavOpen = false" />

    <el-container direction="vertical">
      <AppTopbar :username="displayName" :initials="profileInitials" :roles="auth.roles" :avatar-url="avatarUrl"
        @toggle-nav="mobileNavOpen = !mobileNavOpen" @new-query="router.push('/chat')"
        @preferences="router.push('/preferences')" @logout="logout" />

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
  --el-color-primary: #168463;
  --el-color-primary-light-3: #4ca384;
  --el-color-primary-light-5: #82bda4;
  --el-color-primary-light-7: #b5d7c8;
  --el-color-primary-light-8: #d0e6dc;
  --el-color-primary-light-9: #edf7f2;
  --el-color-primary-dark-2: #0f7253;
  --el-color-success: #168463;
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
  position: relative;
  min-width: 0;
  height: 100vh;
  max-height: 100vh;
  overflow: hidden;
  background: #f6f7fb;
}

.content {
  min-width: 0;
  padding: 34px 38px;
  overflow: auto;
  height: calc(100vh - 76px);
}

.content > * {
  min-width: 0;
}

.content .el-input,
.content .el-select {
  min-width: 0;
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

.el-button--primary {
  --el-button-bg-color: #168463;
  --el-button-border-color: #168463;
  --el-button-text-color: #fff;
  --el-button-hover-bg-color: #0f7253;
  --el-button-hover-border-color: #0f7253;
  --el-button-active-bg-color: #0d644a;
  --el-button-active-border-color: #0d644a;
}

.el-button--primary.is-plain {
  --el-button-bg-color: #edf7f2;
  --el-button-border-color: #b5d7c8;
  --el-button-text-color: #146b50;
  --el-button-hover-bg-color: #dff1e8;
  --el-button-hover-border-color: #82bda4;
  --el-button-hover-text-color: #0f7253;
}

.table-action-neutral {
  color: rgb(96, 98, 102) !important;
}

.mobile-nav-backdrop {
  display: none;
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

@media (max-width: 760px) {
  /* Em telas estreitas, cada linha vira um cartão vertical com o rótulo do campo. */
  .el-table,
  .el-table__inner-wrapper,
  .el-table__body-wrapper,
  .el-table__body-wrapper .el-scrollbar__view {
    width: 100% !important;
    height: auto !important;
    overflow: visible !important;
  }

  .el-table__header-wrapper,
  .el-table__fixed,
  .el-table__fixed-right,
  .el-table__fixed-header-wrapper,
  .el-table__fixed-body-wrapper {
    display: none !important;
  }

  .el-table__body {
    display: block !important;
    width: 100% !important;
  }

  .el-table__body tbody {
    display: grid !important;
    grid-template-columns: repeat(auto-fit, minmax(min(100%, 340px), 1fr));
    align-items: start;
    gap: 12px;
    width: 100%;
  }

  .el-table__body tr {
    display: block !important;
    width: 100%;
    overflow: hidden;
    border: 1px solid #e8ebf1;
    border-radius: 12px;
    background: #fff;
    box-shadow: 0 2px 8px rgba(22, 31, 48, 0.04);
  }

  .el-table__body td.el-table__cell {
    display: grid !important;
    grid-template-columns: minmax(92px, 36%) minmax(0, 1fr);
    align-items: start;
    gap: 8px;
    width: 100%;
    min-height: 38px;
    padding: 8px 11px !important;
    border-bottom: 1px solid #f0f2f5 !important;
    background: transparent !important;
  }

  .el-table__body td.el-table__cell:last-child {
    border-bottom: 0 !important;
  }

  .el-table__body td.el-table__cell::before {
    color: #778499;
    content: '';
    font-size: 11px;
    font-weight: 700;
    line-height: 1.4;
  }

  .el-table__body td .cell {
    min-width: 0;
    padding: 0 !important;
    overflow: visible;
    color: #344054;
    font-size: 12px;
    line-height: 1.45;
    text-align: right;
    white-space: normal;
    overflow-wrap: anywhere;
  }

  .el-table__body td.mobile-label-article::before { content: 'Artigo'; }
  .el-table__body td.mobile-label-rule::before { content: 'Regra'; }
  .el-table__body td.mobile-label-file::before { content: 'Arquivo'; }
  .el-table__body td.mobile-label-name::before { content: 'Nome'; }
  .el-table__body td.mobile-label-sector::before { content: 'Setor'; }
  .el-table__body td.mobile-label-access::before { content: 'Acesso'; }
  .el-table__body td.mobile-label-status::before { content: 'Status'; }
  .el-table__body td.mobile-label-actions::before { content: 'Ações'; }
  .el-table__body td.mobile-label-type::before { content: 'Tipo'; }
  .el-table__body td.mobile-label-schedule::before { content: 'Agendamento'; }
  .el-table__body td.mobile-label-action::before { content: 'Ação'; }
  .el-table__body td.mobile-label-id::before { content: 'ID'; }
  .el-table__body td.mobile-label-user::before { content: 'Usuário'; }
  .el-table__body td.mobile-label-email::before { content: 'E-mail'; }
  .el-table__body td.mobile-label-roles::before { content: 'Papéis'; }
  .el-table__body td.mobile-label-active::before { content: 'Ativo'; }
  .el-table__body td.mobile-label-chunks::before { content: 'Chunks'; }
  .el-table__body td.mobile-label-check-time::before { content: 'Verificação'; }
  .el-table__body td.mobile-label-threshold::before { content: 'Alerta antes'; }
  .el-table__body td.mobile-label-channel::before { content: 'Canal'; }
  .el-table__body td.mobile-label-last-check::before { content: 'Última checagem'; }
  .el-table__body td.mobile-label-reminder::before { content: 'Lembrete'; }
  .el-table__body td.mobile-label-remind-at::before { content: 'Lembrar em'; }
  .el-table__body td.mobile-label-retries::before { content: 'Nº tentativas'; }
  .el-table__body td.mobile-label-started::before { content: 'Início'; }
  .el-table__body td.mobile-label-attempt::before { content: 'Tentativa'; }
  .el-table__body td.mobile-label-http::before { content: 'HTTP'; }
  .el-table__body td.mobile-label-response::before { content: 'Resposta / erro'; }
  .el-table__body td.mobile-label-checked-at::before { content: 'Data'; }
  .el-table__body td.mobile-label-result::before { content: 'Resultado'; }
  .el-table__body td.mobile-label-days-left::before { content: 'Dias restantes'; }
  .el-table__body td.mobile-label-detail::before { content: 'Detalhe'; }
  .el-table__body td.mobile-label-actions .row-actions {
    justify-content: flex-end;
    flex-wrap: wrap;
    white-space: normal;
  }

  .el-table__body td.mobile-label-actions .el-button {
    min-width: 32px;
    min-height: 32px;
  }

  .el-table__body .el-tag {
    max-width: 100%;
    white-space: normal;
  }
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
    padding: 20px 14px;
  }

  .page-heading {
    display: block;
  }

  .page-heading .heading-actions {
    flex-wrap: wrap;
    margin-top: 12px;
  }

  .mobile-nav-backdrop {
    display: block;
    position: fixed;
    inset: 0;
    z-index: 1999;
    background: rgba(15, 23, 42, 0.48);
  }

  .el-dialog {
    max-width: calc(100vw - 24px);
    margin: 12px auto !important;
  }

  .el-drawer {
    max-width: calc(100vw - 12px);
  }

  .el-dialog__body {
    max-height: calc(100dvh - 180px);
    overflow-y: auto;
  }

  .content .header {
    align-items: flex-start;
    flex-wrap: wrap;
    gap: 12px;
  }

  .content .header > :first-child {
    min-width: 0;
  }
}

@media (max-width: 480px) {
  .content {
    padding: 16px 10px;
  }

  .page-heading h1 {
    font-size: 23px;
  }

  .el-button + .el-button {
    margin-left: 4px;
  }
}
</style>
