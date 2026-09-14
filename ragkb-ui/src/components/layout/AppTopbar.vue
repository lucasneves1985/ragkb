<script setup lang="ts">
import { Plus } from '@element-plus/icons-vue'
import UserProfileDropdown from './UserProfileDropdown.vue'

defineProps<{
  username: string
  initials: string
  roles: string[]
}>()

const emit = defineEmits<{
  (e: 'new-query'): void
  (e: 'logout'): void
}>()
</script>

<template>
  <el-header class="topbar">
    <div class="environment">
      <span class="live-dot"></span> Ambiente corporativo
    </div>

    <div class="top-actions">
      <el-button :icon="Plus" round class="new-query" @click="emit('new-query')">
        Nova consulta
      </el-button>

      <UserProfileDropdown
        :username="username"
        :initials="initials"
        :roles="roles"
        @logout="emit('logout')"
      />
    </div>
  </el-header>
</template>

<style scoped>
.topbar {
  height: 76px;
  padding: 0 38px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: rgba(255, 255, 255, 0.88);
  border-bottom: 1px solid #e8ebf1;
  flex-shrink: 0;
}

.environment {
  color: #748096;
  font-size: 13px;
  font-weight: 600;
}

.live-dot {
  display: inline-block;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  margin-right: 7px;
  background: #42c995;
}

.top-actions {
  display: flex;
  align-items: center;
  gap: 20px;
}

.new-query {
  border-color: #dce3eb !important;
  color: #273247 !important;
  font-weight: 700 !important;
}

@media (max-width: 760px) {
  .topbar {
    padding: 0 16px;
    justify-content: flex-end;
  }

  .environment,
  .new-query {
    display: none;
  }
}
</style>
