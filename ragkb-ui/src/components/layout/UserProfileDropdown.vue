<script setup lang="ts">
import { ref, watch } from 'vue'
import { Lock, Setting } from '@element-plus/icons-vue'

const props = defineProps<{
  username: string
  initials: string
  roles: string[]
  avatarUrl?: string
}>()

const avatarFailed = ref(false)
watch(() => props.avatarUrl, () => { avatarFailed.value = false })

const emit = defineEmits<{
  (e: 'logout'): void
  (e: 'preferences'): void
}>()
</script>

<template>
  <el-dropdown trigger="click">
    <button class="profile">
      <img v-if="avatarUrl && !avatarFailed" :src="avatarUrl" alt="Foto do usuário" @error="avatarFailed = true" />
      <span v-else>{{ initials }}</span>
      <div>
        <b>{{ username }}</b>
        <small>{{ roles.join(' · ') }}</small>
      </div>
    </button>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item :icon="Setting" @click="emit('preferences')">Preferências</el-dropdown-item>
        <el-dropdown-item divided :icon="Lock" @click="emit('logout')">
          Sair da conta
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<style scoped>
.profile {
  display: flex;
  align-items: center;
  gap: 9px;
  border: 0;
  background: transparent;
  padding: 0;
  text-align: left;
  cursor: pointer;
}

.profile > span,
.profile > img {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: #dcefe9;
  color: #168463;
  font: 500 12px 'DM Mono';
}

.profile > img {
  object-fit: cover;
}

.profile b,
.profile small {
  display: block;
}

.profile b {
  color: #29354a;
  font-size: 12px;
}

.profile small {
  color: #8490a1;
  font-size: 10px;
  margin-top: 2px;
}
</style>
