<script setup lang="ts">
import { Lock, Plus, Setting } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'

const auth = useAuthStore()
const router = useRouter()

const emit = defineEmits<{ logout: [] }>()

function logout() {
  emit('logout')
}
</script>

<template>
  <el-header class="topbar">
    <div class="environment"><span class="live-dot"></span> Ambiente corporativo</div>
    <div class="top-actions">
      <el-button :icon="Plus" round class="new-query" @click="router.push('/chat')">
        Nova consulta
      </el-button>
      <el-dropdown trigger="click">
        <button class="profile">
          <span>{{ auth.initials }}</span>
          <div>
            <b>{{ auth.username }}</b>
            <small>{{ auth.roles.join(' · ') }}</small>
          </div>
        </button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item :icon="Setting">Preferencias</el-dropdown-item>
            <el-dropdown-item divided :icon="Lock" @click="logout">Sair da conta</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </el-header>
</template>
